# ADR 0001：專案識別方式

- 狀態：已接受
- 日期：2026-11-04
- 對應：ROADMAP R2／P1-07（issue #52 為第 1 步）

## 背景

目前系統用 repo 名稱（`https://github.com/a/utils` 的 `utils`）識別專案：

- `github.com/a/utils` 和 `github.com/b/utils` 會被當成同一個專案。
- `ProjectRepository.findByProjectName` 遇到多筆資料時會丟例外，也可能拿到別人的資料，擁有權檢查因此可能被繞過。
- clone 路徑是 `src/cloneCode/<repoName>`，不同 owner 的同名 repo 會寫進同一個資料夾。

現況裡已經有一部分「共用」的行為：本地已有 clone 時，`GitCloner` 走 pull 並更新資料庫，只新增一筆 `PersonalINFO`；`DataBaseService.deleteDataByProjectName` 先刪自己的 `PersonalINFO`，沒有人擁有時才刪 `Project` 和本地資料夾。本 ADR 把這個行為正式定案，並改用 URL 當唯一鍵。

資料庫裡的舊分析資料不遷移，清掉重新分析（2026-11-03，使用者決定，記錄於 `docs/PROGRESS.md` §6.1）。

## 決策

### 1. 唯一鍵

`project` 新增單一欄位 `repo_url`，存**正規化後的 URL**，全域唯一（加唯一鍵）。例如 `https://github.com/a/utils`。

### 2. 同一個 repo 只有一個 Project（共用一份）

同一個正規化 URL 全域只有一個 `Project`，所有使用者共用同一份分析。第二個使用者加入時，只新增一筆 `PersonalINFO`。

### 3. 舊資料

舊分析資料清空後重新分析，不做遷移。清空範圍：`project`、`files`、`method`、`diff_info`、`personal_info`。

### 4. 正規化規則

先通過 `GitFunction.validateRepoUrl`，再正規化：

1. scheme 固定為 `https`。
2. host 與 path 全部轉小寫。
3. 去掉一個結尾的 `.git`。
4. 去掉一個結尾的 `/`。
5. 輸出固定為 `https://<host>/<owner>/<repo>`。

允許的 host：`github.com`、`gitlab.com`、`bitbucket.org`。路徑格式：`/owner/repo`，名稱只允許英數與 `.` `_` `-`；不允許帳密、port、query、fragment，也不允許 `.`、`..` 段。

**結尾 `/`**：現行驗證器的路徑 pattern 會拒絕結尾 `/`。使用者決定「接受並去掉」，所以第 3 步的實作 PR 要放寬驗證器，允許一個結尾 `/`，正規化時去掉。

輸入與結果對照：

| 輸入 | 正規化結果 |
|---|---|
| `https://GitHub.com/A/Utils.git` | `https://github.com/a/utils` |
| `https://github.com/a/utils` | `https://github.com/a/utils` |
| `https://github.com/a/utils/` | `https://github.com/a/utils`（需第 3 步放寬驗證器） |

被拒絕的輸入：

| 輸入 | 原因 |
|---|---|
| `http://github.com/a/utils` | 只接受 https |
| `git@github.com:a/utils.git` | 不是 https URL |
| `https://example.com/a/utils` | host 不在白名單 |
| `https://github.com/a` | 路徑不是 `/owner/repo` |
| `https://github.com/a/utils?x=1` | 不可帶 query |
| `https://github.com:8443/a/utils` | 不可帶 port |
| `https://github.com/a/.git` | 剝除 `.git` 後名稱為空 |

**小寫化的理由**：這三個 host 的 owner／repo 都不分大小寫，所以大小寫不同的 URL 視為同一個專案。

**`.git` 剝除的邊界**：剝除後名稱為空（例如 `/a/.git`）仍然拒絕。

**實際 clone 用的 URL**：用**正規化後的 URL**，讓共用的 clone 都指向同一個 remote。

### 5. 共用一份的語意

| 情境 | 結論 |
|---|---|
| 加入 | B 加入 A 已分析過的 repo，只新增 `PersonalINFO`，不重新 clone、不重新分析。B 看到的是目前整份歷史，包含 B 加入之前的 commit。 |
| A 刪除 | 只刪 A 的 `PersonalINFO`。B 仍然看得到，資料也還在。 |
| 最後一人刪除 | 沒有任何 `PersonalINFO` 時，才刪 `Project`（含 files／method／diff_info）和本地 clone 資料夾。 |
| A pull（同步） | 更新的是共用的那一份，B 也會看到更新。這是共用的後果，不是 bug。 |
| `commitId` | 共用的專案一律追預設分支的 HEAD，加入時不接受 `commitId`。需要指定 commit 時，改用不存資料庫的臨時分析（`temporaryCloneRepository`）。 |

### 6. 並行與一致性風險

細節留給實作 PR，但實作必須處理：

1. 兩人同時第一次加入同一個 repo：靠 `repo_url` 唯一鍵擋住第二筆 `Project`；實作要處理唯一鍵衝突，之後改走「加入」流程。
2. 刪除與加入同時發生：必須在同一個交易內判斷「是否沒有人擁有」再決定要不要刪 `Project`，避免誤刪剛有人加入的專案。

## 其他選項

| 選項 | 為什麼不選 |
|---|---|
| `(host, owner, repo)` 三欄位複合唯一鍵（ROADMAP P1-07 提過） | 查詢與外鍵都要帶三個欄位，大小寫與 `.git` 的處理仍要在應用層做，沒有比單一正規化 URL 欄位更簡單；使用者選擇單一欄位。 |
| 每個使用者各自一份（不共用） | 同一個 repo 會重複 clone 與分析，浪費儲存空間與時間，clone 路徑也要再加上使用者維度；使用者選擇共用一份。 |
| 保留舊資料並遷移 | 舊資料只有 repo 名稱，無法可靠對應回 URL，且可能已被同名 repo 污染；重新分析比遷移簡單、結果也正確。使用者已決定清掉。 |

## 後果

正面：

- 專案身分明確，`a/utils` 與 `b/utils` 不再撞名，`findByProjectName` 多筆的問題消失。
- 同一個 repo 只 clone、分析一次，節省空間與時間。
- 擁有權檢查依據明確的專案 ID。

負面：

- 所有人共用一份，一人 pull 會影響所有人看到的內容。
- 舊分析資料全部作廢，使用者要重新加入專案。
- 需要處理並行加入與刪除的一致性（見上方風險）。

假設的限制：未來若支援 private repo（`GitCloner` 的 TODO：使用者 GitHub 權限），「共用一份」的假設會失效，需要新增一份 ADR 取代本份。

後續 issue（R2 的第 2 到第 6 步，各跑一次 /ship）：

1. V2 migration：清空分析資料，`project` 加上帶唯一鍵的 `repo_url`，entity 加欄位。
2. 存入正規化 URL 並改查詢（含放寬驗證器的結尾 `/`、clone 使用正規化 URL、加入時不接受 `commitId`）。
3. clone 路徑改用 projectId。
4. API 改用 projectId（保留擁有權檢查）。
5. 前端改用 projectId。
