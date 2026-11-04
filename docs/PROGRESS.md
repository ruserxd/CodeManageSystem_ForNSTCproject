# 進度紀錄

> 這份文件記錄「實際做了什麼、為什麼這樣做、接下來要做什麼」。整體規劃見 [ROADMAP.md](ROADMAP.md)。
>
> **更新方式**：每完成一批 PR 就在「已完成」補上一節，並更新「進度總表」和「下一步」。每一項都附上 issue／PR 編號，細節以 GitHub 上的 PR 描述和 verifier 留言為準。

最後更新：2026-11-04

---

## 1. 總覽

| 項目 | 數字 |
|---|---|
| 本次產品化工作已合併的 PR | 19 個（#4、#6–#8、#10、#11、#13、#14、#16、#19、#22、#24、#27、#28、#31、#33、#35、#37、#39）；另有 2025-02 的舊 PR #1、#2 |
| 已關閉的 issue | 14 個（#3、#5、#9、#12、#15、#17、#18、#21、#23、#29、#30、#32、#36、#38） |
| 尚未處理的 issue | 4 個（#20、#25、#26、#34），見 §5 |
| 後端測試 | 135 個全部通過（開始時是 104 個，其中 3 個失敗，但 CI 仍然顯示成功）；#27 之後的 PR 都沒有改後端 |
| 目前階段 | Phase 1：安全問題 R1、R3–R6 已修正，下一個是 R2（需要先完成的 Flyway 已經做好）。/ship 多 agent 流程已經可以使用（§3.5） |

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
| P1-02 | 寫測試重現 R4–R6 | ✅ R4、R5 先寫測試，並確認修正前會失敗；R6 的驗證是新方法，改以 27 個案例涵蓋 |
| P1-03 | 註冊、登入、使用者列表改用 DTO | 尚未開始 |
| P1-04 | 移除寫死的管理員帳號 | ✅ #10、#11 |
| P1-05 | 臨時分析的 URL 白名單、逾時、repo 大小上限 | ✅ 部分完成 #13、#14（大小上限要等 P1-10） |
| P1-06 | 導入 Flyway | ✅ #22 |
| P1-07 | 專案識別重新設計（R2） | ⏳ 下一步，拆法見 §6 |
| P1-08 ～ P1-17 | 錯誤格式、REST 化、非同步 job、拆分分析器、版本升級、前端等 | 尚未開始 |
| — | 架構審查 A | Phase 1 結束時進行 |

ROADMAP 以外新增的項目：可執行 jar（#23 → #24）、忽略 `frontend/dist`（#8）、verifier agent（#18 → #19）、/ship 多 agent 流程（#28–#39，見 §3.5）。

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

R4、R5 都**先寫測試、確認修正前會失敗**，再修正（錯誤訊息記錄在 PR 描述中）。R3 把原本沒有斷言的測試改成實際驗證行為；R6 的驗證是新方法，沒有「修正前」的版本可以重現，改以大量案例涵蓋。

- **R5 密碼雜湊外洩（#6、#7）**
  - 前端不再顯示 `userPassword`，原本 `UserDetail` 會直接把雜湊顯示在畫面上
  - 後端的 `userPassword` 設為 `WRITE_ONLY` 並排除在 `toString` 之外
  - 修正前的測試錯誤訊息是 `found: '$2a$hash'`
- **R3 寫死的管理員帳號（#10、#11）**
  - 管理員帳密改從 `APP_ADMIN_ACCOUNT`、`APP_ADMIN_PASSWORD` 讀取：兩個都要設定才會建立，任一個空白就不建立
  - 帳號已經存在時，不會因為改了 `APP_ADMIN_PASSWORD` 就更新密碼
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

### 3.5 /ship 多 agent 流程（#28–#39）

這一批 PR 只改 agent／skill 定義（Markdown），沒有改產品程式碼。

| PR | issue | 改了什麼 | 為什麼 |
|---|---|---|---|
| #28 | #21 | 新增 `implementer` agent；/ship 的狀態改放在 GitHub（issue 本文、「📋 驗收標準」留言、PR 與 PR 留言），不再用 `docs/tasks/`；verifier 只保留 PR 模式，在總指揮建立的 detached worktree 上檢驗 | 原本 /ship 缺少實作者；合併條件、worktree 做法、退回流程原本只記在 Claude 的記憶裡，改成寫進 repo |
| #31 | #29 | spec-writer 改讀 prompt 中附上的 issue 內容，輸出由總指揮貼成 issue 留言；S3 的理由更新（pom 已移除 `testFailureIgnore`）；ROADMAP 的分支命名改成 `<type>/<issue 編號>-<英文-slug>` | #28 改了流程，spec-writer 和 ROADMAP 還停在 `brief.md`／`criteria.md` 的舊寫法 |
| #33 | #32 | 新增 `mentor` agent，只有 Read／Grep／Glob 工具；`owner: human` 的子任務（Dockerfile、compose、K8S YAML、nginx）改由 mentor 講解與 review，最後一輪 review 貼成 issue 留言後再 commit | CLAUDE.md 要求這些檔案由使用者自己寫。用工具權限保證 mentor 無法替使用者寫檔 |
| #35 | #30 | SKILL.md ④ 把 ⚪ 分成「無法執行」和「待確認」：前者由總指揮補好環境再重驗，後者由總指揮查證；⑤ 的合併條件改成 ⚪ 不能只靠說明理由合併 | 原本「PASS（有建議）」只說明 🟡 怎麼處理，總指揮寫一句理由就能合併沒驗過的項目 |
| #37 | #36 | verifier.md 要求「標 ⚪」的地方都寫明是哪一種；SKILL.md 的 FAIL 分支補上：重驗前一併補好 ⚪「無法執行」的環境 | #35 的 verifier 指出：沒寫標籤的 ⚪ 無法分流；FAIL 時沒補環境會多跑一輪 |
| #39 | #38 | SKILL.md 的 FAIL 分支補上：交給 implementer（或 mentor）之前先查證 ⚪「待確認」，查證出的 🔴 在同一輪一起修 | #36 驗收標準列為範圍外的項目。不先查證的話，查證後才發現的 🔴 會多用一次退回 |

#35、#37、#39 都是前一個 PR 的 verifier 或 spec-writer 列在範圍外、另開 issue 後接著處理的。

---

## 4. 已建立的工作流程

```
issue（為什麼要改、驗收條件）
  → 分支（fix/、chore/、docs/…）
  → 實作與自我驗證（先寫會失敗的測試）
  → PR（Closes #N）
  → verifier 獨立檢驗，報告留言在 PR 上
  → CI 綠燈
  → squash 合併（自 #8 起；#4、#6、#7 是 merge commit）
```

| 規則 | 來源 |
|---|---|
| 一個 PR 只做一件事；改動不超過 3 個檔案，超過就拆成多個 PR | CLAUDE.md、使用者要求「以小內容去做修改」 |
| Dockerfile、compose、K8S YAML 由使用者撰寫，agent 只解釋觀念和 review | CLAUDE.md |
| 合併條件：實作方自己驗證過、verifier 判定 PASS、CI 綠燈，三項都滿足 | 使用者決定 |
| verifier 檢驗時，在 PR 的 commit 上另外建立 worktree，並比對 `headRefOid` | 第一次檢驗 #22 時，Agent 工具的 worktree 是從 main 建立的；verifier 發現 HEAD 不一致後停止（那次的報告沒有貼到 PR 上） |
| verifier 在範圍外的發現另開 issue，不塞進目前的 PR | #20、#21、#25、#26 都是這樣來的；之後的 #30、#34、#36 也是 |
| /ship 唯一的人工關卡是 ② 的驗收標準核准；verifier 判 PASS 且 CI 綠燈後由總指揮直接合併 | 使用者決定（2026-11-03 授權），寫在 `.claude/skills/ship/SKILL.md` |
| 本機測試一律使用 JDK 21：`JAVA_HOME=$(/usr/libexec/java_home -v 21 --failfast)` | 跟 CI 一致 |

相關檔案：
- `.claude/agents/verifier.md`：檢驗者
- `.claude/agents/spec-writer.md`：訂標準，用於 /ship
- `.claude/agents/implementer.md`：實作 `owner: agent` 的子任務，用於 /ship（#28）
- `.claude/agents/mentor.md`：講解與 review `owner: human` 的子任務，只讀不寫（#33）
- `.claude/skills/ship/SKILL.md`：/ship 流程，由總指揮依序呼叫上面四個 agent

---

## 5. 待處理的 issue

| issue | 內容 | 建議時機 |
|---|---|---|
| #20 | CLAUDE.md 有過時描述（`testFailureIgnore`、JaCoCo 路徑、超級帳號 `"123"`）；spec-writer S3 的部分已在 #31 修正 | 和 P1-17 一起處理 |
| #25 | CI 沒有實際驗證 Flyway migration 和 validate | R2 之前或同時處理，因為 R2 會新增 V2 migration |
| #26 | h2 被打包進正式 jar | 和 #25 一起考慮 |
| #34 | 一個子任務有多個 `owner: human` 檔案時，mentor review 與 `Reviewed-in` 怎麼對應 | Phase 3 開始前 |

---

## 6. 下一步

### 6.1 R2：專案識別重新設計（P1-07）

**已經決定**：資料庫裡的舊分析資料**清掉、重新分析**，不做遷移（2026-11-03，使用者決定）。這個決定之後要寫進 P1-07 的 ADR，並同步更新 ROADMAP §7 的「待決事項」。

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

- [ ] **處理舊的管理員帳號**：跑過舊版的資料庫裡還會有 `123/123`。
  - 如果這個帳號沒有任何專案，可以直接執行 ``DELETE FROM `user` WHERE user_account = '123';``
  - 如果它擁有專案，會被外鍵 `personal_info.user_id` 擋下來。可以先刪掉它在 `personal_info` 的資料，或者直接改掉它的密碼。依 R2 的決定，專案資料之後都會清掉重新分析
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
| 同一個分支不能同時被兩個 worktree checkout；不加 `--detach` 的 worktree 會佔住 main | git 的限制 | worktree 一律用 `--detach` 建立（#28） |
| agent frontmatter 裡的 ` #21`、`owner: human` 讓 YAML 解析出錯 | ` #` 會被當成註解，`: ` 會被當成 mapping | 含這些字元的值加上引號，並用 YAML parser 確認（#28、#33） |
| 總指揮寫一句理由，就能合併 verifier 沒驗到的項目 | 合併條件只寫了「說明不處理的理由」，沒有區分 🟡 和 ⚪ | ⚪ 要補環境重驗或查證，不能只寫理由（#35） |
