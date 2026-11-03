# 進度紀錄

> 這份文件記錄「實際做了什麼、為什麼這樣做、接下來要做什麼」。整體規劃見 [ROADMAP.md](ROADMAP.md)。
>
> **更新方式**：每完成一批 PR 就在「已完成」補上一節，並更新「進度總表」和「下一步」。每一項都附上 issue／PR 編號，細節以 GitHub 上的 PR 描述和 verifier 留言為準。

最後更新：2026-11-03

---

## 1. 總覽

| 項目 | 數字 |
|---|---|
| 已合併的 PR | 12 個（#4、#6–#8、#10、#11、#13、#14、#16、#19、#22、#24） |
| 已關閉的 issue | 8 個（#3、#5、#9、#12、#15、#17、#18、#23） |
| 尚未處理的 issue | 4 個（#20、#21、#25、#26），見 §5 |
| 後端測試 | 135 個全部通過（開始時是 104 個，其中 3 個失敗，但 CI 仍然顯示成功） |
| 目前階段 | Phase 1：安全問題 R1、R3–R6 已修正，下一個是 R2（需要先完成的 Flyway 已經做好） |

---

## 2. 進度總表（對照 ROADMAP）

### 2.1 Phase 1 現況盤點的問題

| ID | 問題 | 狀態 | issue | PR |
|---|---|---|---|---|
| R1 | CI 永遠不會失敗 | ✅ 完成 | — | #4 |
| R2 | 專案用 repo 名稱識別 | ⏳ 下一步 | — | — |
| R3 | 寫死的管理員帳號 `123/123` | ✅ 完成 | #9 | #10、#11 |
| R4 | 註冊 API 可以覆蓋既有使用者 | ✅ 完成（暫時的止血，DTO 屬於 P1-03） | #15 | #16 |
| R5 | API 回應與 log 洩漏密碼雜湊 | ✅ 完成（暫時的止血，DTO 屬於 P1-03） | #5 | #6、#7 |
| R6 | clone 任意 URL（SSRF、讀取本機檔案、路徑穿越） | ✅ 完成（repo 大小上限要等 P1-10） | #12 | #13、#14 |
| A6 | 用 `ddl-auto=update` 管理 schema | ✅ 完成 | #17 | #22 |
| A10 | 建置環境沒有固定（JDK、Maven Wrapper） | ✅ 完成 | #3 | #4 |
| A1–A5、A7–A9 | 其他架構問題 | 尚未開始 | — | — |

### 2.2 Phase 1 任務清單

| ID | 任務 | 狀態 |
|---|---|---|
| P1-01 | CI 改為測試失敗就失敗 | ✅ #4 |
| P1-01b | 修復 Maven Wrapper、鎖定 JDK | ✅ #4 |
| P1-02 | 寫測試重現 R4–R6 | ✅ 每個修正 PR 都先寫測試、確認修正前會失敗 |
| P1-03 | 註冊、登入、使用者列表改用 DTO | 尚未開始 |
| P1-04 | 移除寫死的管理員帳號 | ✅ #10、#11 |
| P1-05 | 臨時分析的 URL 白名單與逾時 | ✅ #13、#14 |
| P1-06 | 導入 Flyway | ✅ #22 |
| P1-07 | 專案識別重新設計（R2） | ⏳ 下一步，拆法見 §6 |
| P1-08 ～ P1-17 | 錯誤格式、REST 化、非同步 job、拆分分析器、版本升級、前端等 | 尚未開始 |
| — | 架構審查 A | Phase 1 結束時進行 |

ROADMAP 以外新增的項目：可執行 jar（#23 → #24）、忽略 `frontend/dist`（#8）、verifier agent（#18 → #19）。

---

## 3. 已完成（依時間）

### 3.1 建置環境與 CI（#4）

| 改了什麼 | 為什麼 |
|---|---|
| 移除 pom 的 `testFailureIgnore=true` | 開始時有 3 個測試失敗，CI 卻仍然是綠燈。這個值寫在 `<configuration>` 裡，用 `-D` 也蓋不掉，所以直接移除 |
| 修正浮現的 3 個測試：刪掉 2 個過時的測試，`deleteCloneFileTest` 改用 `@TempDir` | 舊版會刪掉開發機上真正的 `src/cloneCode/`；static 的 `path` 也會外洩到其他測試 |
| CI 加上 `pull_request` 觸發，actions 升到 v4 | 在合併**之前**就能擋下失敗的測試 |
| 把 `.mvn/` 移到 `backend/`，`mvnw` 加上執行權限 | 拆分前後端資料夾時（6a1c767），`.mvn/` 被遺留在根目錄 |
| 宣告 Lombok 為 annotation processor，用 enforcer 鎖定 JDK 21–26 | JDK 23 起 javac 預設不再自動執行 annotation processor，所以 23 以上的版本會出現 `cannot find symbol log` |

### 3.2 安全修正

每一項都**先寫測試、確認修正前會失敗**，再修正。

- **R5 密碼雜湊外洩（#6、#7）**
  - 前端不再顯示 `userPassword`，原本 `UserDetail` 會直接把雜湊顯示在畫面上
  - 後端的 `userPassword` 設為 `WRITE_ONLY` 並排除在 `toString` 之外
  - 修正前的測試錯誤訊息是 `found: '$2a$hash'`
- **R3 寫死的管理員帳號（#10、#11）**
  - 管理員帳密改從 `APP_ADMIN_ACCOUNT`、`APP_ADMIN_PASSWORD` 讀取；沒有設定時，不建立任何管理員
- **R6 任意 URL（#13、#14）**
  - `GitFunction.validateRepoUrl`：只允許 https、只允許 github.com／gitlab.com／bitbucket.org、路徑必須是 `/owner/repo`
  - 用 27 個測試案例涵蓋 `file://`、內網位址、偽造的 host、`..` 等情況：4 個合法、22 個不合法，再加上 null
  - clone 加上 60 秒逾時；驗證失敗時回 400
- **R4 註冊覆蓋既有使用者（#16）**
  - 註冊時只取白名單內的欄位，建立新的 `MyUser`
  - 修正前的測試顯示 `save()` 收到 `userId = 1`，也就是會透過 JPA merge 覆蓋第 1 位使用者

### 3.3 資料庫 schema 管理（#22）

- 加入 Flyway。`V1__baseline.sql` 是讓 Hibernate 在 MySQL 上實際建表後匯出的，包含 6 張 `*_seq` 表
- `ddl-auto` 改為 `validate`；舊版資料庫用 `baseline-on-migrate` 直接標記在 V1
- 用暫時的 MySQL 9.2 驗證：
  - 空資料庫會套用 V1
  - 舊版資料庫會標記 baseline，而且保留原本的資料
  - verifier 另外用 schema diff 確認 V1 和 Hibernate 產生的 schema 一致

### 3.4 其他

- **可執行 jar（#24）**：pom 原本沒有 `spring-boot-maven-plugin`，jar 只有 100 KB，Dockerfile 的 `java -jar` 無法執行。修正後 jar 是 70 MB，可以啟動
- **忽略 `frontend/dist`（#8）**
- **verifier agent（#19）**：見 §4

---

## 4. 已建立的工作流程

```
issue（為什麼要改、驗收條件）
  → 分支（fix/、chore/、docs/…）
  → 實作與自我驗證（先寫會失敗的測試）
  → PR（Closes #N）
  → verifier 獨立檢驗，報告留言在 PR 上
  → CI 綠燈
  → squash 合併
```

| 規則 | 來源 |
|---|---|
| 一個 PR 只做一件事；改動不超過 3 個檔案，超過就拆成多個 PR | CLAUDE.md、使用者要求「以小內容去做修改」 |
| Dockerfile、compose、K8S YAML 由使用者撰寫，agent 只解釋觀念和 review | CLAUDE.md |
| 合併條件：實作方自己驗證過、verifier 判定 PASS、CI 綠燈，三項都滿足 | 使用者決定 |
| verifier 檢驗時，在 PR 的 commit 上另外建立 worktree，並比對 `headRefOid` | #22 的經驗：Agent 工具的 worktree 是從 main 建立的 |
| verifier 在範圍外的發現另開 issue，不塞進目前的 PR | #20、#21、#25、#26 都是這樣來的 |
| 本機測試一律使用 JDK 21：`JAVA_HOME=$(/usr/libexec/java_home -v 21 --failfast)` | 跟 CI 一致 |

相關檔案：
- `.claude/agents/verifier.md`：檢驗者
- `.claude/agents/spec-writer.md`：訂標準，用於 /ship
- `.claude/skills/ship/SKILL.md`：/ship 流程，目前還缺 implementer（#21）

---

## 5. 待處理的 issue

| issue | 內容 | 建議時機 |
|---|---|---|
| #20 | CLAUDE.md 與 spec-writer S3 有過時描述（`testFailureIgnore`、JaCoCo 路徑、超級帳號 `"123"`） | 和 P1-17 一起處理 |
| #21 | /ship 缺少 implementer agent | 要開始使用 /ship 之前 |
| #25 | CI 沒有實際驗證 Flyway migration 和 validate | R2 之前或同時處理，因為 R2 會新增 V2 migration |
| #26 | h2 被打包進正式 jar | 和 #25 一起考慮 |

---

## 6. 下一步

### 6.1 R2：專案識別重新設計（P1-07）

**已經決定**：資料庫裡的舊分析資料**清掉、重新分析**，不做遷移（2026-11-03）。

預計拆成幾個小 PR，實作時可能再調整：

1. **V2 migration**：清空 project、files、method、diff_info、personal_info；`project` 新增 `repo_url` 欄位並加上唯一鍵；entity 加上對應的欄位
2. **以 URL 識別專案**：clone 與分析時存入正規化後的 URL，查詢改用 URL
3. **clone 路徑**：改用 projectId，或 `host/owner/repo`，不再用 repo 名稱
4. **API 與前端改用 projectId**：保留擁有權檢查

### 6.2 Phase 1 剩下的任務（建議順序）

1. **P1-03**：DTO，取代 R4、R5 的暫時止血
2. **P1-08**：統一錯誤格式（ProblemDetail）
3. **P1-09**：REST 化 API，加入 OpenAPI
4. **P1-10**：非同步分析 job（之後 LLM 的 job 也會沿用）
5. **P1-11**：拆分 `GitDiffAnalyzer`
6. **P1-12**：clone 路徑和 log 改由設定決定
7. **P1-13**：LAZY fetch
8. **P1-14**：升級到 Java 25 和 Spring Boot 4.x
9. **P1-15、P1-16**：前端
10. **P1-17**：文件，含 #20
11. **架構審查 A**

Phase 2（LLM）和 Phase 3（K8s）的規劃，見 ROADMAP §3、§4。

---

## 7. 需要使用者手動處理的事

- [ ] **刪除舊的管理員帳號**：跑過舊版的資料庫裡還會有 `123/123`，執行 `DELETE FROM user WHERE user_account = '123';`
- [ ] **需要管理員帳號時**，在 `.env` 裡設定 `APP_ADMIN_ACCOUNT`、`APP_ADMIN_PASSWORD`
- [ ] **本機 MySQL**（官方安裝檔，9.0.1，port 3306）第一次用新版後端啟動時：
  - 正常情況下，Flyway 會自動標記 baseline
  - 如果 validate 失敗，就刪掉**整個 database** 重建，或者連 `flyway_schema_history` 一起刪
- [ ] **（選擇性）** 用 `.claude/settings.json` 限制 verifier 的 Bash 權限，例如禁止 `git push`、`gh pr merge`。這是權限設定，由使用者決定

---

## 8. 過程中學到的事

| 現象 | 原因 | 之後怎麼避免 |
|---|---|---|
| 3 個測試失敗，CI 仍然是綠燈 | `testFailureIgnore=true` | 驗證時看 Surefire 的統計數字，不要只看 BUILD SUCCESS |
| `./mvnw` 無法使用 | 搬資料夾時遺漏了隱藏的 `.mvn/` | 搬移後用 `git ls-tree` 確認檔案都在 |
| JDK 23 以上的版本編譯失敗 | javac 預設不再自動執行 annotation processor | 在 pom 中明確宣告 `annotationProcessorPaths` |
| `java_home -v 21` 找不到時，靜默改用 JDK 23 | 找不到時預設不會報錯 | 加上 `--failfast`，並在報告中附上 `Java version` |
| `export X=$(cmd) \|\| exit 1` 不會中止 | `export` 本身的 exit code 是 0 | 先賦值、檢查失敗，再 export |
| verifier 拿到的不是 PR 的程式碼 | Agent 工具的 worktree 是從 main 建立的 | 自己在 PR 的 commit 上建立 detached worktree |
| 打包出來的 jar 無法執行 | 沒有宣告 `spring-boot-maven-plugin` | 已修正（#24） |
