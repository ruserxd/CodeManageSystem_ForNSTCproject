# 進度紀錄

> 這份文件記錄「實際做了什麼、為什麼這樣做、接下來要做什麼」。整體規劃見 [ROADMAP.md](ROADMAP.md)。
>
> **更新方式**：每完成一批 PR 就在「已完成」補上一節，並更新「進度總表」和「下一步」。每一項都附上 issue／PR 編號，細節以 GitHub 上的 PR 描述和 verifier 留言為準。

最後更新：2026-11-08

---

## 1. 總覽

| 項目 | 數字 |
|---|---|
| 本次產品化工作已合併的 PR | 39 個（#4、#6–#8、#10、#11、#13、#14、#16、#19、#22、#24、#27、#28、#31、#33、#35、#37、#39、#41、#42、#48、#50、#53、#55、#57、#60、#64、#65、#67、#70、#72、#75、#78、#83–#87）；另有 2025-02 的舊 PR #1、#2。2026-11-08 以 `gh pr list --state merged` 取得 |
| 已關閉的 issue | 33 個（#3、#5、#9、#12、#15、#17、#18、#21、#23、#25、#26、#29、#30、#32、#36、#38、#40、#43、#44、#45、#49、#52、#54、#56、#58、#62、#66、#69、#71、#77、#79、#81、#82） |
| 尚未處理的 issue | 13 個（#20、#34、#46、#47、#51、#59、#61、#63、#68、#73、#74、#76、#80），見 §5；另有進行中的 #88（本次文件同步） |
| 後端測試 | 185 個全部通過（PR #60、#64 的 `./mvnw test` 輸出：`Tests run: 185, Failures: 0, Errors: 0`）；開始時是 104 個，其中 3 個失敗，但 CI 仍然顯示成功。#64 之後沒有改後端，未重新統計 |
| 目前階段 | Phase 1：安全問題 R1、R3–R6 已修正，R2 進行中（ADR 0001、V2 migration、`normalizeRepoUrl` 已完成，其餘步驟見 §6.1）。另有一條提前的 compose 部署軌道（2026-11-04 使用者決定，見 ROADMAP §4.3）：#43–#45 已完成，下一步是 #46、#47。/ship 多 agent 流程已經可以使用（§3.5、§3.9） |

---

## 2. 進度總表（對照 ROADMAP）

### 2.1 Phase 1 現況盤點的問題

| ID | 問題 | 狀態 | issue | PR |
|---|---|---|---|---|
| R1 | CI 永遠不會失敗 | ✅ 完成 | — | #4 |
| R2 | 專案用 repo 名稱識別 | ⏳ 進行中（ADR、V2 migration、`normalizeRepoUrl` 已完成） | #52、#54、#56 | #53、#55、#57 |
| R3 | 寫死的管理員帳號 `123/123` | ✅ 完成 | #9 | #10、#11 |
| R4 | 註冊 API 可以覆蓋既有使用者 | ✅ 完成（暫時的止血，DTO 屬於 P1-03） | #15 | #16 |
| R5 | API 回應與 log 洩漏密碼雜湊 | ✅ 完成（暫時的止血，DTO 屬於 P1-03） | #5 | #6、#7 |
| R6 | clone 任意 URL（SSRF、讀取本機檔案、路徑穿越） | ✅ 完成（repo 大小上限要等 P1-10） | #12 | #13、#14 |
| A6 | 用 `ddl-auto=update` 管理 schema | ✅ 完成（#42 起 CI 實際驗證 migration） | #17、#25 | #22、#42 |
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
| P1-07 | 專案識別重新設計（R2） | ⏳ 進行中：#53（ADR）、#55（V2 migration）、#57（`normalizeRepoUrl`）完成，其餘依 ADR 0001 的後續步驟，見 §6.1 |
| P1-08 ～ P1-17 | 錯誤格式、REST 化、非同步 job、拆分分析器、版本升級、前端等 | 尚未開始 |
| — | 架構審查 A | Phase 1 結束時進行 |

ROADMAP 以外新增的項目：可執行 jar（#23 → #24）、忽略 `frontend/dist`（#8）、verifier agent（#18 → #19）、/ship 多 agent 流程（#28–#39，見 §3.5；之後的調整見 §3.9）、移除 h2（#26 → #48）、修正 PMD plugin 版本（#58 → #60）、關閉 `Git` 資源（#62 → #64）。

compose 部署軌道（ROADMAP §4.3，提前進行）：P3-01 完成於 #65（含 #67 修正）、P3-02 完成於 #72、compose 調整完成於 #75；P3-10 對應的 #46 與自動部署 #47 尚未開始。

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

這一批 PR 只改 agent／skill 定義與 ROADMAP（Markdown），沒有改產品程式碼。

| PR | issue | 改了什麼 | 為什麼 |
|---|---|---|---|
| #28 | #21 | 新增 `implementer` agent；/ship 的狀態改放在 GitHub（issue 本文、「📋 驗收標準」留言、PR 與 PR 留言），不再用 `docs/tasks/`；verifier 只保留 PR 模式，在總指揮建立的 detached worktree 上檢驗 | 原本 /ship 缺少實作者；合併條件、worktree 做法、退回流程原本只記在 Claude 的記憶裡，改成寫進 repo |
| #31 | #29 | spec-writer 改讀 prompt 中附上的 issue 內容，輸出由總指揮貼成 issue 留言；S3 的理由更新（pom 已移除 `testFailureIgnore`）；ROADMAP 的分支命名改成 `<type>/<issue 編號>-<英文-slug>` | #28 改了流程，spec-writer 和 ROADMAP 還停在 `brief.md`／`criteria.md` 的舊寫法 |
| #33 | #32 | 新增 `mentor` agent，只有 Read／Grep／Glob 工具；`owner: human` 的子任務（Dockerfile、compose、K8S YAML、nginx）改由 mentor 講解與 review，最後一輪 review 貼成 issue 留言後再 commit | CLAUDE.md 要求這些檔案由使用者自己寫。用工具權限保證 mentor 無法替使用者寫檔 |
| #35 | #30 | SKILL.md ④ 把 ⚪ 分成「無法執行」和「待確認」：前者由總指揮補好環境再重驗，後者由總指揮查證；⑤ 的合併條件改成 ⚪ 不能只靠說明理由合併 | 原本「PASS（有建議）」只說明 🟡 怎麼處理，總指揮寫一句理由就能合併沒驗過的項目 |
| #37 | #36 | verifier.md 要求「標 ⚪」的地方都寫明是哪一種；SKILL.md 的 FAIL 分支補上：重驗前一併補好 ⚪「無法執行」的環境 | #35 的 verifier 指出：沒寫標籤的 ⚪ 無法分流；FAIL 時沒補環境會多跑一輪 |
| #39 | #38 | SKILL.md 的 FAIL 分支補上：交給 implementer（或 mentor）之前先查證 ⚪「待確認」，查證出的 🔴 在同一輪一起修 | #36 驗收標準列為範圍外的項目。不先查證的話，查證後才發現的 🔴 會多用一次退回 |

#35、#37、#39 都是先前 PR 的 verifier 或 spec-writer 列在範圍外、另開 issue 後接著處理的：#30 來自 #28 的 verifier，#36 來自 #35 的 verifier，#38 來自 #36 的驗收標準。

### 3.6 CI、相依套件與後端小修（#42、#48、#60、#64）

| PR | issue | 改了什麼 | 為什麼 |
|---|---|---|---|
| #42 | #25 | 新增 `FlywaySchemaValidationTest`（`@SpringBootTest` ＋ Testcontainers MySQL），CI 移除 `ddl-auto=create-drop` 的覆寫與 MySQL service，改成 MySQL 8.0／8.4 的 matrix | 原本 CI 的 `create-drop` 蓋掉了 `validate`，而且沒有任何測試會啟動完整 context，所以 Flyway migration 與 Hibernate validate 從來沒被驗過 |
| #48 | #26 | 刪掉 `pom.xml` 的 `h2` 相依 | 主程式和測試都沒用到 H2（schema 用 MySQL 的 Flyway migration，H2 驗不出相容性）；正式 jar 少一個要追 CVE 的套件 |
| #60 | #58 | `pom.xml` `<reporting>` 的 `maven-pmd-plugin` 從 `4.0.0-beta-1`（Maven Central 上不存在）改為 `3.28.0` | Docker multi-stage build（#43）執行 `./mvnw dependency:go-offline` 會因找不到這個 artifact 失敗；`test`／`package` 不解析 reporting plugin，所以先前沒被發現 |
| #64 | #62 | `GitPuller.pullLocalRepository` 改用 try-with-resources 管理 `Git` | pull 失敗提早 return 或 `call()` 丟例外時 `Git` 不會被關閉；`deleteGitRepository` 的重複 close 因 3 檔上限拆到 #63（尚未處理） |

### 3.7 R2：專案識別（#53、#55、#57）

依 ADR 0001（`docs/adr/0001-project-identity.md`）的決定：唯一鍵改用正規化的 `repo_url`，同一個 repo 全域只有一個 Project，舊資料清掉重新分析。

| PR | issue | 改了什麼 | 為什麼 |
|---|---|---|---|
| #53 | #52 | 新增 ADR 0001；ROADMAP §7 的「現有資料的遷移」改成已決定 | R2 要先把識別方式、正規化規則、舊資料處理與後續步驟定案，再動 schema |
| #55 | #54 | `V2__project_repo_url.sql`：清空 `project`、`files`、`method`、`diff_info`、`personal_info`，`project` 新增 `repo_url` 與唯一鍵；`Project` 加欄位；`FlywaySchemaValidationTest` 新增 5 個測試 | 這是破壞性的 migration，套用後要手動刪除 `backend/src/cloneCode/` 底下舊的 clone；`repo_url` 先允許 NULL，因為現有程式碼建立 Project 時還不會寫入 |
| #57 | #56 | `GitFunction`：`validateRepoUrl` 允許一個結尾 `/`，新增 `normalizeRepoUrl`（host 與 path 轉小寫、去結尾 `/` 與 `.git`），新增 25 個參數化案例 | 提供 ADR 規定的正規化函式；還沒接到 clone 流程（ADR 後續第 2 步才接） |

### 3.8 compose 部署軌道（#65、#67、#72、#75）

ROADMAP §4.3 的前三步。Dockerfile、compose、nginx 設定由使用者撰寫（owner: human），Claude 負責講解與 review，PR 描述附有 mentor review 紀錄。

| PR | issue | 改了什麼 | 為什麼 |
|---|---|---|---|
| #65 | #43 | backend Dockerfile：改用 `eclipse-temurin`（建置 `21-jdk`、執行 `21-jre`）、multi-stage build（容器內 `./mvnw package`）、非 root 的 `appuser`；新增 `.dockerignore` | 官方 `openjdk` 映像已從 Docker Hub 移除；建置不再依賴主機的 JDK 與 Maven；後端會寫入 `src/cloneCode`、`src/log`，這兩個目錄預先建立並授權給 `appuser` |
| #67 | #66 | frontend Dockerfile 啟用 corepack 並複製 Yarn 4 設定；`.dockerignore` 排除 `node_modules` 等 | `node:24-alpine` 內建 Yarn 1，無法執行 `packageManager: yarn@4.12.0` 的專案；macOS 的 `node_modules` 原生套件會經 `COPY . .` 蓋掉 Linux 版本 |
| #72 | #44 | frontend Dockerfile 改為 multi-stage（`yarn build` → nginx 只提供 `dist/`），新增 `frontend/nginx.conf`（SPA 路由 fallback 到 `index.html`），`ARG VITE_API_URL` | 不再用 Vite dev server 對外服務（ROADMAP A9）；SPA 路由重新整理才不會 404 |
| #75 | #45 | `docker-compose.yml`：backend 用 named volume `backend_clonecode`、`backend_log`；frontend 改用 `build.args`；backend、frontend 加 GHCR `image:` 名稱；只有 nginx 對外開 80。`nginx/nginx.conf` 的 `location /` 轉 `frontend:80`。`.env.example` 的 `VITE_API_URL` 改為空值 | 讓 compose 成為可部署版本：資料持久化、只開一個對外 port、映像名稱與 #46 的 CI 推送一致、前端與 API 走同源 |

### 3.9 /ship 後續調整（#50、#70、#78、#83–#87）

這一批 PR 只改 agent／skill 定義與 `.claude/skills/ship/scripts/`，沒有改產品程式碼。

| PR | issue | 改了什麼 | 為什麼 |
|---|---|---|---|
| #50 | #49 | 4 個 agent 定義的 frontmatter 加上 `model` 與 `effort`（spec-writer、implementer、mentor 為 sonnet，verifier 為 opus） | 依角色降低 token 消耗 |
| #70 | #69 | `SKILL.md` 與 4 個 agent 定義改寫為英文，規則語意不變；新增「給使用者看的產出一律用繁體中文」；agent 之間比對的字串（`📋 驗收標準`、`PASS`／`FAIL`、🔴🟡⚪ 等）維持中文 | 定義檔改英文；舊的 issue／PR 因固定字串不變而不受影響 |
| #78 | #71 | spec-writer、implementer、mentor 的 owner 規則：會建置／推送映像或部署的 workflow 視為 `owner: human`，依內容判斷，純測試 workflow 仍是 `agent` | 為 #46、#47 做準備，使用者的 workflow 由使用者撰寫 |
| #83 | #77 | verifier 新增基礎設施靜態檢查（compose、Dockerfile、workflow）；`SKILL.md` ⑤ 新增部署類 PR 的合併後驗證與煙霧測試 | 讓 /ship 能驗證部署類任務；煙霧測試的 `/api/login` 預期（回 200 且 body 含 `"success":false`）由 `@WebMvcTest` 確認，尚未在 compose／實驗室環境實測 |
| #84 | #79 | verifier 新增「🔴 必須附證據」規則；mentor 的 🔴 附使用者可執行的指令；spec-writer 對既有行為的斷言要引用 `file:line` | #77 的「`/api/login` 回 4xx」是沒有查程式碼的斷言，一路傳到驗收標準 |
| #85 | #81 | `SKILL.md` ⑤ 新增「回顧」步驟：合併後在 PR 貼 `## /ship 回顧` 留言 | 借鏡 pstack reflect，讓每次執行的教訓留下紀錄 |
| #86 | #82 | `SKILL.md` ⑤ 交付時加入 how／why／其他可行做法的講解（只講 `owner: agent` 的改動） | 借鏡 pstack teach，輔助學習 |
| #87 | #80（`Refs`） | 新增 `verify-worktree.sh`、`check-frontmatter.sh`，`SKILL.md` 對應改寫 | 把 /ship 最常重複的兩組步驟（等 `headRefOid` 並建 worktree、檢查 frontmatter）寫成腳本；其餘候選留在 #80 |

#41 是先前 PR 的文件補述（PROGRESS 補上 /ship 流程），沒有另外列在這裡。

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
| /ship 唯一的人工關卡是 ② 的驗收標準核准 | 使用者決定，記在 issue #21 的設計決策留言（2026-11-04），寫在 `.claude/skills/ship/SKILL.md` ② |
| verifier 判 PASS 且 CI 綠燈後，由總指揮直接合併 | 使用者 2026-11-03 授權，寫在 `.claude/skills/ship/SKILL.md` ⑤ |
| 本機測試一律使用 JDK 21：`JAVA_HOME=$(/usr/libexec/java_home -v 21 --failfast)` | 跟 CI 一致 |
| owner 涵蓋部署 workflow：會建置／推送映像或部署的 `.github/workflows/*.yml` 視為 `owner: human`，依內容判斷，純測試 workflow（如 `backend_test.yml`）仍是 `agent` | #71／#78，SKILL.md ③ 第 2 步 |
| 基礎設施檔案的靜態檢查：只在改到 compose、Dockerfile、workflow 時，verifier 執行 `docker compose config -q`、`docker build --platform linux/amd64`（不 push）、`actionlint`；缺工具或 daemon 沒啟動標 ⚪ 無法執行，不自行安裝 | #77／#83，verifier.md 第 7 項 |
| 合併後驗證：合併會觸發建置／推送映像或部署的 workflow run 時，用 `gh run watch --exit-status` 等結果；真的部署到主機時再做煙霧測試；失敗不得宣告交付，以 revert PR 或部署上一個映像 tag 回退，不 force-push | #77／#83，SKILL.md ⑤ 第 3 步 |
| 判 🔴 必須附實際執行的證據（指令與輸出）；只靠閱讀的發現標 ⚪ 待確認；spec-writer 對既有行為的斷言要引用 `file:line`；mentor 的 🔴 附使用者可自己跑的驗證指令 | #79／#84，verifier.md「Evidence rule for 🔴」 |
| 合併後回顧：每次合併後在 PR 貼 `## /ship 回顧` 留言，必填 (a)–(f)，不是人工關卡；有價值的內容之後整理進本文件 §8 | #81／#85，SKILL.md ⑤ 第 4 步 |
| how／why 講解：交付時只針對 `owner: agent` 的改動，講 how、why、其他可行做法，挑 1 至 2 個點，沒有值得講的點就寫一行說明 | #82／#86，SKILL.md ⑤ 第 5 步 |
| 兩支腳本：`verify-worktree.sh`（等 `headRefOid` 與本機 HEAD 一致後建立 verifier 的 detached worktree）、`check-frontmatter.sh`（改到 `.claude/agents/` 或 `.claude/skills/` 時，push 前檢查 frontmatter） | #80／#87，SKILL.md ③ 第 3 步、④ 第 1 步 |
| /ship 的 `SKILL.md` 與 4 個 agent 定義改寫為英文，規則語意不變；給使用者看的產出仍一律用繁體中文，agent 之間比對的固定字串維持中文 | #69／#70，SKILL.md「Language」 |

相關檔案：
- `.claude/agents/verifier.md`：檢驗者
- `.claude/agents/spec-writer.md`：訂標準，用於 /ship
- `.claude/agents/implementer.md`：實作 `owner: agent` 的子任務，用於 /ship（#28）
- `.claude/agents/mentor.md`：講解與 review `owner: human` 的子任務，只讀不寫（#33）
- `.claude/skills/ship/SKILL.md`：/ship 流程，由總指揮依序呼叫上面四個 agent
- `.claude/skills/ship/scripts/verify-worktree.sh`：建立 verifier 用的 worktree（#87）
- `.claude/skills/ship/scripts/check-frontmatter.sh`：檢查 agent 與 skill 定義的 frontmatter（#87）

SKILL.md 與 4 個 agent 定義現在是英文（#70）；給使用者看的內容仍是繁體中文。

---

## 5. 待處理的 issue

2026-11-08 以 `gh issue list --state open` 取得，共 13 個（不含進行中的 #88）。

| issue | 內容 | 建議時機 |
|---|---|---|
| #20 | CLAUDE.md 與 spec-writer S3 仍描述已移除的 `testFailureIgnore` | 和 #61、P1-17 一起處理 |
| #34 | 一個子任務有多個 `owner: human` 檔案時，mentor review 與 `Reviewed-in` 怎麼對應 | Phase 3 開始前 |
| #46 | push main 時建置 backend／frontend 映像並推到 GHCR（P3-10） | compose 部署軌道的下一步（ROADMAP §4.3） |
| #47 | 實驗室 self-hosted runner 自動以 compose 部署 | 接在 #46 之後 |
| #51 | SKILL.md 寫明呼叫 agent 時不要帶 model／effort 參數 | /ship 小改動，可隨時處理 |
| #59 | `ApiController.deleteUser` 有不必要的括號，`pmd:check` 失敗 | 後端小修，可隨時處理 |
| #61 | CLAUDE.md 仍寫 Surefire 設了 `testFailureIgnore=true`，但 pom 已移除 | 和 #20 一起處理 |
| #63 | `deleteGitRepository` 對 JGit Repository 重複 close | 後端小修（由 #64 拆出） |
| #68 | `vite server.open` 在容器內造成 `spawn xdg-open ENOENT` | 前端小修 |
| #73 | 前端 nginx 的 `/assets/` 不存在時回 404，並加上快取標頭 | 前端小修（由 #72 帶出） |
| #74 | 新增 `.editorconfig` 統一各類檔案的縮排與檔尾換行 | 小改動，可隨時處理 |
| #76 | `.env.example` 佔位值照抄會啟動失敗（`JWT_SECRET` 長度、DB 帳號與 compose 不符） | 在 #47 之前處理較好 |
| #80 | /ship 重複的規則改寫成腳本（第 1 批已由 #87 完成，其餘候選待處理） | 之後依回顧結果挑選 |

---

## 6. 下一步

### 6.0 提前進行：compose 部署軌道

使用者在 2026-11-04 決定：compose 部署提前到 Phase 1 完成之前；K8s（P3-05 之後）維持原本的順序。#43–#45 已完成，接下來是 #46（CI 建置映像推到 GHCR）→ #47（實驗室 self-hosted runner 部署）。兩者的 workflow 屬 `owner: human`，實作注意事項見 ROADMAP §4.3。這條線與 Phase 1 的順序互不影響，Phase 1 剩餘任務的順序不變。

### 6.1 R2：專案識別重新設計（P1-07）

**已經決定**：資料庫裡的舊分析資料**清掉、重新分析**，不做遷移（2026-11-03，使用者決定），已寫入 ADR 0001（#53）。

已完成：ADR（#53）、V2 migration（#55）、`normalizeRepoUrl` 與放寬結尾 `/`（#57，屬 ADR 後續第 2 步的一部分）。

剩餘步驟依 ADR 0001「後續 issue」清單（R2 的第 2 到第 6 步，各跑一次 /ship；R2 第 1 步是 ADR 本身，所以清單的第 1 項 V2 migration 是 R2 第 2 步）：

- R2 第 3 步：**存入正規化 URL 並改查詢**，含放寬驗證器的結尾 `/`（#57 已完成）、clone 使用正規化 URL、加入時不接受 `commitId`、加入時不再 pull 與增量分析（#57 只提供函式，尚未接到 clone 流程）
- R2 第 4 步：**clone 路徑**改用 projectId
- R2 第 5 步：**API 改用 projectId**（保留擁有權檢查）
- R2 第 6 步：**前端改用 projectId**

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
| skill frontmatter 裡的 ` #21`（SKILL.md 的 `argument-hint`）、agent frontmatter 裡的 `owner: human`（mentor.md 的 description）讓 YAML 解析出錯 | ` #` 會被當成註解，`: ` 會被當成 mapping | 含這些字元的值加上引號，並用 YAML parser 確認（#28、#33） |
| 總指揮寫一句理由，就能合併 verifier 沒驗到的項目 | 合併條件只寫了「說明不處理的理由」，沒有區分 🟡 和 ⚪ | ⚪ 要補環境重驗或查證，不能只寫理由（#35） |
| 在 M4 上用 `--platform linux/amd64` 建置 backend 穩定失敗（`./mvnw dependency:go-offline` 解壓縮 Maven 時 `Function not implemented`），原生 arm64 則成功（PR #83 verifier 實測，記錄在 #46 留言，2026-11-08） | Docker Desktop 的 amd64 模擬環境問題，不在 Dockerfile 本身 | amd64 映像在 CI 原生建置；本機只用原生 arm64 驗證 Dockerfile（ROADMAP §4.3）。若 Dockerfile 日後寫入 amd64 專用內容，要在 verifier.md 補「原生失敗原因是架構專用內容時標 ⚪ 待確認」（#46 留言） |
| #77 驗收標準寫「`/api/login` 回 4xx」，實際回 200（#84 的動機） | 這是沒有查程式碼的斷言，經過 issue、spec-writer 一路傳下去 | 對既有行為的斷言要引用 `file:line`，否則寫「待實作時實測」，🔴 要附實際執行的證據（#84） |
| 每次建立 verifier worktree 都要手動等 `headRefOid`、拆指令（#78、#83、#84、#85 都重複），沙盒又拒絕內嵌 `git` 的複合指令（#85 回顧） | 步驟固定但每次手動操作，且沙盒擋複合指令 | 寫成腳本，以絕對路徑單獨呼叫（#87 已完成，沙盒接受這個形式；#87 回顧）。其餘腳本候選（`mvn21.sh`、`read-issue.sh`、`merge-pr.sh`）留在 #80 |
| worktree 沙盒會拒絕：python heredoc 內容提到 git、`for` 迴圈、相對路徑接 `; echo`、`printf … > file`、多段 heredoc（#85、#87 回顧） | 沙盒無法證明這些複合指令只作用在自己的 worktree | 改用 Edit／Write 工具、單一指令，或以絕對路徑單獨呼叫腳本；被拒絕就說明，不繞過（SKILL.md「Rules」） |
| 本機 `main` ref 過時，`git diff main` 與實際不符（#85 回顧） | 本機沒有 fetch，ref 落後遠端 | 比對一律用 `origin/main`；verifier 用 `gh api .../commits/main` 確認 |
| 加了 commit 後 PR 描述沒有同步，連續 3 次被 verifier 標 🟡（PR #84、#86、#87；#86、#87 回顧） | 描述裡的「驗收條目對應」是手動維護，沒有檢查 | 目前是 #80 剩下的候選（PR 描述 commit 對應檢查），尚未實作；在那之前，每次 push 新 commit 後手動重讀 PR 描述 |
| implementer 照驗收條目的「預期」做，漏掉條目本文多寫的一句（PR #86 的 C6，`9e832a0` 補上） | spec-writer 的條目本文與「預期」不一致 | spec-writer 寫條目時，本文和「預期」要一致，不要只在本文多寫要求。尚未改 `spec-writer.md`，等下次改它時一起處理（#86 回顧）；來源是 #86 回顧，不是新規則 |
| bash 腳本在 `set -u` 下報 `unbound variable`（#87 開發中發現） | 變數後面直接接全形標點（如 `$target（`）會被當成變數名的一部分 | 一律寫成 `${var}`（#87 的 `ab88983` 已改） |
| `verify-worktree.sh` 的 `yarn install --immutable` 分支沒有實測（#87 回顧） | 近 60 個已合併 PR 都沒動 `package.json`／`yarn.lock`，找不到樣本 PR | 第一次遇到改相依套件的前端 PR 時，實際跑一次並確認腳本行為 |
