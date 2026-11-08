# CodeManageSystem 產品化路線圖

> 版本：v1（2026-11-03）
> 目標：把研究原型改造成可以長期維護、可以部署到雲端的商業級軟體，並加入以本地 LLM 為核心的程式碼歷史分析功能。

---

## 0. 總覽

| 階段 | 主題 | 產出 | 結束時的關卡 |
|---|---|---|---|
| **Phase 1** | 架構修正與技術棧現代化 | 安全、可測試、可擴充的基礎 | 🔍 **架構審查 A** |
| **Phase 2** | Python LLM 服務與功能 ①–⑤ | 摘要、分類、演化敘事、語意搜尋、歷史問答 | 🔍 **架構審查 B**（做完 ①② 後）<br>🔍 **架構審查 C**（做完 ⑤ 後） |
| **Phase 3** | 容器化、K8s、雲端部署 | 實驗室 GPU ＋ 雲端網站的混合部署 | 🔍 **上線前審查 D** |

**為什麼是這個順序**：Phase 2 和 Phase 3 都建立在 Phase 1 的地基上。如果在舊架構上直接加 LLM，「用專案名稱當識別」「分析在 HTTP 請求裡同步執行」這類問題會被複製到每個新功能裡，到時候再修，成本是現在的好幾倍。

**其他可行做法**：先容器化（Phase 3）再修架構，好處是很早就能練 K8s。但 Phase 1 會改到資料庫 schema 和 API，部署設定得重寫一遍，所以不建議。

**順序的例外**：使用者在 2026-11-04 決定，compose 部署（#43–#47）提前到 Phase 1 完成之前；K8s（P3-05 之後）維持原順序。詳見 §4.3。

---

## 1. 目標架構

```
                         ┌─────────────── 雲端（日後 AWS）或實驗室主機 ───────────────┐
 瀏覽器 ──HTTPS──▶ Ingress ─┬─▶ frontend（靜態檔，nginx）                              │
                         │  └─▶ backend（Spring Boot，唯一對外的 API）                  │
                         │        │  ├─ MySQL 8.4（業務資料＋job 表）                   │
                         │        │  └─ Qdrant（向量，功能 ④⑤）                        │
                         │        │                                                   │
                         │        ▲  /internal/llm-jobs（worker 主動拉 job、回傳結果）  │
                         └────────┼───────────────────────────────────────────────────┘
                                  │ 由 worker 主動向外連線（實驗室不需要開任何對外的 port）
                         ┌────────┴──────── 實驗室 RTX 5080 主機 ────────┐
                         │  llm-worker（Python）──▶ Ollama（GPU）        │
                         └──────────────────────────────────────────────┘
```

### 核心設計決策（每一項都要寫成 ADR，見 §6）

| # | 決策 | 為什麼 | 其他可行做法 |
|---|---|---|---|
| D1 | **Spring Boot 是唯一的對外 API**，Python 不直接對瀏覽器開放 | JWT 驗證和擁有權檢查只需要實作一次；LLM 讀到的每段程式碼都已經過授權 | 前端直接打 FastAPI：延遲比較低，但兩邊都要實作驗證 |
| D2 | **LLM 工作採非同步 job**：backend 建立 job，worker 拉取執行 | 本地推論可能要幾十秒；GPU 一次只能處理少量請求，需要排隊 | 同步 HTTP 呼叫：最簡單，但只適合 demo |
| D3 | **由 worker 主動拉 job**（pull），不是 backend 推給 worker（push） | 實驗室在校園網路裡面，雲端連不進去。由 worker 往外連，同一套設計在本機、實驗室、AWS 都能用 | Redis／RabbitMQ 佇列：功能較完整，但要另外打通網路（VPN／Tailscale） |
| D4 | **Python 透過 OpenAI 相容 API 呼叫 Ollama**（`/v1/chat/completions`） | 之後換成 vLLM 或雲端模型時，只要改 base URL | 用 `ollama` 官方 Python 套件：功能比較多，但會被綁在 Ollama 上 |
| D5 | **一張資料表只有一個服務能寫**：worker 不直接連 MySQL，結果透過 backend 的 internal API 寫回 | 避免兩種語言各自維護一份 schema；權限和驗證集中在 backend | worker 直接寫 DB：少一次 HTTP，但 schema 會耦合在兩個服務之間 |
| D6 | **Schema 用 Flyway 管理**，關閉 `ddl-auto=update` | 正式環境不能讓 Hibernate 猜測要怎麼改表；可以回溯、可以審查 | Liquibase：功能相當，用 XML/YAML 撰寫 |

---

## 2. Phase 1：架構修正與技術棧現代化

### 2.1 現況盤點：發現的問題

依照風險排序。標「待驗證」的項目，第一步是**寫一個測試把問題重現出來**，確認問題存在後再修。

#### 🔴 嚴重（安全／資料正確性）

| ID | 問題 | 位置 | 影響 |
|---|---|---|---|
| R1 | **CI 永遠不會失敗**：`testFailureIgnore=true` 也套用在 CI 上 | `pom.xml`、`.github/workflows/backend_test.yml` | 後面每一次升級都缺少可靠的安全網 |
| R2 | **專案是用 repo 名稱識別的**：`github.com/a/utils` 和 `github.com/b/utils` 會被當成同一個專案；`findByProjectName` 遇到多筆資料時會丟例外，也可能拿到別人的資料 | `GitDiffAnalyzer`、`ProjectRepository`、`ApiController`、clone 路徑 | 資料互相污染，擁有權檢查可能被繞過 |
| R3 | **寫死的管理員帳號** `123/123`，啟動時自動建立 | `UserService.addSuperAccount`、`MyPostConstruct` | 任何部署出去的環境都有一組公開的管理員密碼 |
| R4 | **註冊時直接綁定 Entity**（`@RequestBody MyUser`）。如果請求裡帶了 `userId`，`save()` 會變成更新既有使用者（待驗證） | `ApiController.register`、`UserService.userRegister` | 可能被拿來覆蓋其他帳號 |
| R5 | **回應中可能包含密碼雜湊**：`MyUser` 沒有 `@JsonIgnore` 密碼欄位，登入回應和 `getAllUsers` 都直接回傳 Entity（待驗證） | `MyUser`、`SessionResponse` | 洩漏 bcrypt hash |
| R6 | **臨時分析可以 clone 任意 URL**，包括 `file://` 和內網位址；沒有大小和時間限制 | `/get-method-diff-temporary`、`GitCloner` | SSRF、磁碟被塞滿、讀到伺服器本機的檔案 |

#### 🟠 架構

| ID | 問題 | 影響 |
|---|---|---|
| A1 | 分析大型 repo 時在 HTTP 請求中**同步執行**，可能長達數分鐘 | 請求逾時、執行緒被佔滿；也無法顯示進度 |
| A2 | 有狀態的資料放在**本機相對路徑**（`src/cloneCode/`、`src/log/`） | 容器重啟後就消失，K8s 也無法水平擴充 |
| A3 | API 直接回傳 **JPA Entity**、用 `ResponseEntity<?>` 回傳字串錯誤，也沒有統一的錯誤格式 | 前後端耦合，改一個欄位就可能壞掉；也會連帶造成 R5 |
| A4 | 用 **GET 執行有副作用的操作**（`/deleteData`、`/pullProject`、`/addSuperAccount`），命名也不一致 | 違反 HTTP 語意，可能被預取或快取觸發；不利於產生 OpenAPI 文件 |
| A5 | `GitDiffAnalyzer` 有 685 行，**分析、Git I/O、組裝 Entity 混在一起** | 很難測試；Phase 2 也需要「只分析、不存檔」的版本 |
| A6 | 用 `ddl-auto=update` 管理 schema | 見 D6 |
| A7 | `Project.files` 使用 **EAGER fetch** | 大型專案一次載入整棵物件樹 |
| A8 | 前端同時存在**三套 UI 系統**（antd、flowbite-react、styled-components），還有 CRA 的遺留設定（`eslintConfig: react-app`、`babel-eslint`、`web-vitals`） | bundle 變肥，風格不一致 |
| A9 | 前端 Dockerfile 在正式環境執行 **`vite dev`**，而且沒有複製 `yarn.lock` | 效能差、建置結果不可重現（這是 Phase 3 的 human 任務） |
| A10 | **建置環境沒有固定**：`./mvnw` 無法使用，只能用系統的 `mvn`；Homebrew 的 `mvn` 預設使用 JDK 26，Lombok 在上面無法產生 `log` 等程式碼，導致編譯失敗 | 「我電腦上可以跑」的問題；新加入的開發者第一步就會卡住 |

### 2.2 技術棧升級目標

> 版本號在實作時要用官方發布頁再核對一次（`spec-writer` 的固定標準）。原則是**跟上目前仍有官方支援的 LTS／GA 線**。

| 項目 | 現在 | 目標 | 主要注意事項 |
|---|---|---|---|
| Java | 21 | **25 LTS** | 21 也仍是 LTS，可以只升級 runtime 而不使用新語法 |
| Spring Boot | 3.5.16（開源支援已結束，pom 裡在手動覆寫 CVE 修補的版本） | **4.x 最新 GA** | Spring Framework 7、Jackson 3（套件改成 `tools.jackson`）、Jakarta EE 11；要看官方 migration guide |
| MySQL | 8.0（已 EOL） | **8.4 LTS** | 8.4 預設停用 `mysql_native_password`，compose 的 `command` 要改 |
| JaCoCo／PMD 外掛 | 0.8.12／4.0.0-beta-1 | 支援 Java 25 的正式版 | 舊版 JaCoCo 不認得新的 class 檔格式 |
| 密碼雜湊 | `jbcrypt 0.4`（自己包一層） | Spring Security `PasswordEncoder`（BCrypt 或 Argon2） | 舊的 hash 可以用 `DelegatingPasswordEncoder` 相容 |
| React | 18 | **19** | `react-dom` 的 API 有變；`react-cookie` 的相容性要確認 |
| React Router | 6 | **7** | 可以先用 v6 future flags 漸進遷移 |
| antd | 5 | **6** | 移除 flowbite-react 和 styled-components（A8） |
| Vite | 6 | 目前的 major 版本 | 搭配新版 `@vitejs/plugin-react` |
| Node（建置用） | 24 | 24 LTS | — |
| 前端語言 | JavaScript | **TypeScript（漸進導入）** | 先從 `api/` 和型別定義開始 |

### 2.3 任務清單

每一項都是一次 `/ship` 任務，每次改動不超過 3 個檔案（超過時再切小）。依照順序執行，因為後面的任務依賴前面的安全網。

| ID | 任務 | 對應問題 | 依賴 |
|---|---|---|---|
| P1-01 | 移除 pom 的 `testFailureIgnore`，本機與 CI 一致：測試失敗就建置失敗（在 pom `<configuration>` 寫死的值無法用 `-D` 覆寫）；一併修正因此浮現的 3 個過時測試；CI 加上 `pull_request` 觸發 | R1 | — |
| P1-01b | 修復 Maven Wrapper（`.mvn/wrapper/` 遺失、`mvnw` 沒有執行權限），並用 `maven-enforcer-plugin` 鎖定 JDK 版本範圍 | A10 | — |
| P1-02 | 寫測試重現 R4、R5、R6，確認問題存在 | R4–R6 | P1-01 |
| P1-03 | 註冊、登入、使用者列表改用 Request/Response DTO | R4、R5、A3 | P1-02 |
| P1-04 | 移除寫死的管理員帳號，改成啟動時從環境變數建立第一個管理員（沒有設定就不建立） | R3 | P1-01 |
| P1-05 | 臨時分析加上 URL 白名單（只允許 `https://` 和指定的 host）、repo 大小上限、逾時 | R6 | P1-02 |
| P1-06 | 導入 Flyway：用現有的 schema 產生 `V1__baseline.sql`，關閉 `ddl-auto` | A6 | P1-01 |
| P1-07 | **專案識別重新設計**：用 `(host, owner, repo)` 或正規化後的 URL 當唯一鍵；clone 路徑改用 projectId；API 改用 `projectId` | R2 | P1-06 |
| P1-08 | 統一錯誤格式（RFC 9457 `ProblemDetail`＋`@RestControllerAdvice`） | A3 | P1-03 |
| P1-09 | REST 化 API：`/api/v1/projects/{id}`、`DELETE`、`POST /projects/{id}/sync`；加入 springdoc OpenAPI | A4 | P1-07、P1-08 |
| P1-10 | **非同步分析 job**：`analysis_job` 表（狀態：QUEUED/RUNNING/DONE/FAILED）＋背景執行器；前端改成輪詢進度 | A1 | P1-09 |
| P1-11 | 拆分 `GitDiffAnalyzer`：`CommitWalker`（Git I/O）、`MethodExtractor`（JavaParser）、`MethodDiffer`（diff-utils）、`ProjectAssembler`（組裝 Entity） | A5 | P1-02 |
| P1-12 | clone 路徑和 log 改由設定決定（`app.storage.clone-root`）；log 只輸出到 stdout | A2 | P1-07 |
| P1-13 | `Project.files` 改成 LAZY，用 fetch join 或 DTO projection 處理需要的查詢 | A7 | P1-11 |
| P1-14 | 升級 Java 25 ＋ Spring Boot 4.x ＋ MySQL 8.4，移除 pom 裡的 CVE 版本覆寫 | 2.2 | P1-01～P1-13 都穩定之後 |
| P1-15 | 前端：清理相依套件、升級 React 19／Router 7／antd 6、改用 ESLint flat config、加入 Vitest | A8、2.2 | — |
| P1-16 | 前端：用 TanStack Query 管理 server state，axios 改成有型別的 API client（TypeScript 起點） | A3 | P1-09、P1-15 |
| P1-17 | 更新 CLAUDE.md（目前寫的還是 Spring Boot 3.4）與 README | — | Phase 1 結尾 |

**為什麼 P1-14（大版本升級）排在後面**：一次只改一件事，壞掉時才知道是誰造成的。先修好架構、補齊測試，升級時才能靠測試抓到行為改變。
**其他可行做法**：先升級再修，好處是新寫的程式碼一開始就用新 API；缺點是升級和重構的錯誤會混在一起，很難分辨。

> 🔍 **架構審查 A**（Phase 1 結束）→ 見 §5

---

## 3. Phase 2：Python LLM 服務與功能 ①–⑤

### 3.1 執行環境：實驗室 RTX 5080

- **VRAM 16 GB** 的實際限制：
  - 程式碼模型選 **14B 參數左右，Q4／Q5 量化**（約 9–11 GB），留空間給 KV cache（context）
  - 嵌入模型選小型的（< 1 GB），可以跟主模型同時載入
  - 20B 以上的模型通常要犧牲 context 長度，或者部分跑在 CPU 上，速度會明顯變慢
- 模型名稱與參數**寫在設定檔裡**（`LLM_MODEL`、`EMBED_MODEL`），不寫死在程式中，方便做 A/B 比較
- 在 Ollama 設定 `OLLAMA_NUM_PARALLEL` 和 `OLLAMA_MAX_LOADED_MODELS`，控制同時處理幾個請求、載入幾個模型
- 開發時：Mac 本機的 Ollama 跑小模型（Docker 容器在 Mac 上用不到 Metal GPU）；功能完成後再到 5080 上跑正式模型

### 3.2 Python 服務技術棧

| 用途 | 選擇 | 為什麼 |
|---|---|---|
| 套件與虛擬環境 | **uv** | 速度快，lockfile 可重現，目前業界的主流做法 |
| 版本 | Python 3.13 | 主流套件都已支援 |
| Web／API | **FastAPI**（health、metrics、同步的小工具 API） | 自動產生 OpenAPI，與 Pydantic 整合 |
| 資料模型／結構化輸出 | **Pydantic v2** | 同一個 model 同時用來驗證 LLM 的 JSON 輸出、產生 JSON schema |
| HTTP | **httpx**（async） | worker 拉 job 和呼叫 LLM 都用它 |
| LLM client | `openai` SDK，指向 Ollama 的 `/v1` | 見 D4 |
| 向量資料庫 | **Qdrant** | 獨立的服務，有官方 Helm chart，適合練習 K8s 的 StatefulSet |
| 測試／品質 | pytest、ruff、mypy | — |
| 評估 | 自建的標註資料集＋腳本（見 3.4） | — |

**其他可行做法（向量資料庫）**：
- PostgreSQL＋pgvector：可以順便把 MySQL 換掉，但這個遷移太大
- MySQL 9 的 `VECTOR` 型別：目前功能和生態還不成熟
- Chroma：上手最快，但不太適合正式環境

### 3.3 服務結構

```
llm-service/
├── pyproject.toml / uv.lock
├── app/
│   ├── main.py            FastAPI：/health、/metrics
│   ├── worker.py          迴圈：向 backend 拉 job → 執行 → 回傳結果
│   ├── llm/client.py      OpenAI 相容的 client、逾時、重試
│   ├── tasks/             每個功能一個模組：summarize、classify、narrate、embed、answer
│   ├── prompts/           prompt 模板（有版本號，跟結果一起存）
│   └── schemas.py         Pydantic：job、各功能的輸出格式
├── eval/                  標註資料集與評估腳本
└── tests/
```

Backend 這一側要新增：
- `llm_job` 表
- `/internal/llm-jobs/next`、`/internal/llm-jobs/{id}/result`，用 **service token** 驗證，跟使用者 JWT 分開
- 存放結果的欄位或資料表

### 3.4 功能規格

| # | 功能 | 輸入 → 輸出 | 關鍵技術 | 驗收重點 |
|---|---|---|---|---|
| ① | **Diff 摘要** | 一筆 `DiffInfo` → 一到兩句中文摘要 | prompt 設計、批次處理、快取（同一個 diff 不重算） | 分析完成後自動排入 job；前端在 DiffCard 顯示 |
| ② | **變更分類** | 一筆 `DiffInfo` → `{type, confidence, reason}`，type 是 bugfix／refactor／feature／style／test 其中之一 | Ollama 的 `format`（JSON schema）＋ Pydantic 驗證、驗證失敗時重試 | **人工標註 ≥100 筆**，量測 accuracy 和每個類別的 F1；專案層級的統計圖表 |
| ③ | **方法演化敘事** | 一個方法的所有 ①② 結果 → 一段演化說明 | map-reduce（拿 ① 的摘要再做一次摘要，不重讀原始 diff） | 歷史很長時也不會超過 context；敘事中引用了 commit SHA |
| ④ | **語意搜尋** | 自然語言查詢 → 相關的方法清單 | 對方法內容＋① 的摘要做 embedding、寫入 Qdrant、payload 帶 projectId 做隔離 | **只搜得到自己擁有的專案**（D1）；準備 20 組查詢計算 recall@10 |
| ⑤ | **歷史問答** | 問題 → 有引用來源的回答 | RAG（④ 檢索＋①③ 當 context）、SSE 串流回應 | 每個回答都附上 commit／方法的連結；找不到依據時回答「不知道」 |

**為什麼是這個順序**：每一個功能都重用前一個的產出。② 用到 ① 的管線；③ 用到 ① 的摘要；④ 用到 ① 的摘要當 embedding 的素材；⑤ 用到 ④ 的檢索結果。

**其他可行做法**：先做 ④⑤（最炫的功能）。但沒有 ①② 建立起 job 管線和評估機制，RAG 的品質無從量測。

### 3.5 任務清單

| ID | 任務 |
|---|---|
| P2-01 | **ADR**：LLM 整合架構（D1–D5 正式定案） |
| P2-02 | Backend：`llm_job` 表（Flyway）＋ internal API ＋ service token |
| P2-03 | Python：專案骨架（uv、ruff、pytest、FastAPI health） |
| P2-04 | Python：worker 迴圈＋ LLM client（逾時、重試、退避） |
| P2-05 | 功能 ①：摘要 task ＋ 寫回結果 ＋ 前端顯示 |
| P2-06 | 功能 ②：分類 task ＋ 結構化輸出 |
| P2-07 | 功能 ②：標註工具／資料集＋評估腳本 |
| — | 🔍 **架構審查 B** |
| P2-08 | 功能 ③：演化敘事 |
| P2-09 | Qdrant 整合＋功能 ④：embedding job ＋ 搜尋 API |
| P2-10 | 功能 ⑤：RAG 問答＋ SSE 串流 |
| P2-11 | 可觀測性：記錄每個 job 的模型、prompt 版本、token 數、耗時 |
| — | 🔍 **架構審查 C** |

---

## 4. Phase 3：容器化、K8s、雲端部署

> 依照 CLAUDE.md：這一階段的 Dockerfile、compose、K8S YAML **由開發者本人撰寫**，Claude 負責解釋觀念和 review。`/ship` 會把這些子任務標記為 `owner: human`。

### 4.1 學習路徑

| 步驟 | 內容 | 學習重點 |
|---|---|---|
| P3-01 | 重寫 backend 的 Dockerfile：multi-stage build（在容器裡建置 jar）、`eclipse-temurin` 映像、非 root 使用者（已完成：#65） | 映像分層、建置快取、為什麼 `openjdk` 映像已被棄用 |
| P3-02 | 重寫 frontend 的 Dockerfile：建置階段 `vite build` → 用 nginx 提供靜態檔（修正 A9）（已完成：#72） | dev server 和正式環境的差別 |
| P3-03 | 新增 llm-service 的 Dockerfile（uv 官方映像） | Python 映像的瘦身做法 |
| P3-04 | 擴充 compose：加入 llm-service、Qdrant、healthcheck、內部網路，只有 nginx 對外開 port | 網路隔離、`expose` 和 `ports` 的差別 |
| P3-05 | 本機 K8s（k3d 或 kind）：把 compose 的每個服務翻成 Deployment＋Service | 見下方對照表 |
| P3-06 | ConfigMap／Secret、PVC（MySQL、clone 目錄、Qdrant） | 有狀態服務的處理方式 |
| P3-07 | readiness／liveness probe（Spring Boot Actuator 的 health groups） | K8s 用 probe 取代 compose 的 `depends_on` |
| P3-08 | Ingress＋TLS（cert-manager） | 取代手寫的 nginx 反向代理 |
| P3-09 | 打包：Kustomize（dev/prod overlay）或 Helm | 同一份設定如何部署到多個環境 |
| P3-10 | CI/CD：GitHub Actions 只建置 `linux/amd64` 映像 → 推到 GHCR（實驗室主機確認是 amd64，不做 multi-arch；M4 上用 amd64 模擬建置 backend 會失敗，所以 amd64 映像要在 CI 原生產生，見 §4.3） | M4 是 arm64、實驗室與 CI runner 是 amd64，本機只用原生 arm64 驗證 Dockerfile |
| P3-11 | 實驗室 5080 主機：安裝 NVIDIA driver、Container Toolkit；worker 和 Ollama 用 compose 或 k3s 執行 | GPU 容器化 |
| P3-12 | 雲端（AWS）：網站部署到雲端，worker 留在實驗室主動連線（D3） | 混合部署 |

**compose 與 K8s 的對照**

| compose | K8s |
|---|---|
| 一個 service | Deployment＋Service |
| `environment` / `.env` | ConfigMap／Secret |
| `volumes` | PersistentVolumeClaim（MySQL、Qdrant 建議用 StatefulSet） |
| `depends_on` + `healthcheck` | readiness／liveness probe |
| `ports`（nginx 對外） | Ingress |

### 4.2 AWS 方案比較（P3-12 前要寫 ADR）

| 方案 | 每月成本（粗估） | 學習價值 | 適合的時機 |
|---|---|---|---|
| **單台 EC2＋k3s** | 低 | 高（K8s 全部自己管） | 學習、展示 ← **建議先從這裡開始** |
| ECS Fargate＋RDS | 中 | 中（AWS 原生的做法） | 不想管 K8s 時 |
| EKS＋RDS | 高（光 control plane 就有固定月費） | 高（業界標準） | 正式商業營運 |

GPU 推論留在實驗室。雲端上的 GPU 機器費用很高，而且 D3 的 pull 設計本來就讓 worker 可以放在任何地方。

> 🔍 **上線前審查 D** → 見 §5

### 4.3 compose 部署軌道（提前）

使用者在 2026-11-04 決定：compose 部署提前到 Phase 1 完成之前，K8s（P3-05 之後）維持原本的順序。目標是 push 到 `main` 後，自動在實驗室機器上以 compose 更新並部署。

| 順序 | issue | 內容 | 狀態 |
|---|---|---|---|
| 1 | #43 | 重寫 backend Dockerfile（P3-01，PR #65） | 已完成 |
| 2 | #44 | 重寫 frontend Dockerfile，nginx 提供靜態檔（P3-02，PR #72） | 已完成 |
| 3 | #45 | compose 調整為可部署版本：named volume、只開 nginx port、GHCR 映像名稱（PR #75） | 已完成 |
| 4 | #46 | CI：push 到 `main` 且測試通過後建置 backend、frontend 映像並推到 GHCR（P3-10） | 未完成 |
| 5 | #47 | 實驗室 self-hosted runner 執行 `docker compose pull && docker compose up -d` 自動部署 | 未完成 |

**2026-11-08 規劃審查整理的實作注意事項**（做 #46、#47 時要處理）：

1. **`.env` 會被 `actions/checkout` 清掉**：checkout 預設會執行 `git clean -ffdx`，放在工作目錄裡的 `.env` 會消失。要把 `.env` 放在工作目錄以外的固定路徑，compose 用 `--env-file` 指定。
2. **volume 名稱跟 compose project name 走**：project name 預設取自目錄名，runner 的工作目錄變了，`backend_clonecode`、`backend_log` 與 MySQL 的 volume 就會換成新的，舊資料看起來像不見。用頂層 `name:` 或 `-p` 固定 project name。
3. **test → build → deploy 怎麼串**：做法一是同一個 workflow 內用 `needs` 串 job，簡單且能保證順序；做法二是另一個 workflow 用 `workflow_run` 接在測試之後，可以拆開檔案，但要自己處理「測試失敗時不部署」的條件與 commit 對應。
4. **部署後煙霧測試有限制**：目前沒有免登入的 health endpoint，而 `/actuator/**` 需要 ADMIN，無法直接拿來探測。可行的探測是 `GET /` 回 200，以及 `POST /api/login {}` 的回應：`@WebMvcTest` 確認回 200 且 body 含 `"success":false`（PR #83）；尚未在 compose／實驗室環境實測。
5. **部署前先用 `mysqldump` 備份資料庫**：R2 的 V2 migration 合併後會在部署時自動執行，並清掉實驗室既有資料，沒有備份就無法復原。
6. **`IMAGE_TAG` 回退與映像清理**：CI 同時推 commit SHA 與 `latest`，compose 的 image 改成 `:${IMAGE_TAG:-latest}`，並在 `.env.example` 新增 `IMAGE_TAG`，出問題時可以指定舊版本回退。每次部署會累積舊映像，要定期執行 `docker image prune`，避免實驗室機器磁碟被塞滿。
7. **deploy job 的安全與並行限制**：加上 `concurrency` 避免兩次部署同時執行；只允許 `push: main` 與 `workflow_dispatch` 觸發，不可用 `pull_request`（repo 公開，fork PR 可能在實驗室機器上執行程式碼）；確認 GHCR package 的可見性（repo 公開時預設公開，映像內不能有密鑰）；`.env`（`JWT_SECRET`、DB 密碼）只放在實驗室機器上，不進版本控制、不進映像。

**架構限制**：實驗室主機確認是 amd64（#46 留言，2026-11-08），只建置 `linux/amd64`，不做 multi-arch。M4 上用 amd64 模擬建置 backend 會穩定失敗（`./mvnw dependency:go-offline` 解壓縮 Maven 時出現 `Function not implemented`；#46 留言，2026-11-08，來自 PR #83 verifier 的實測），同一個 Dockerfile 用原生 arm64 建置則成功，所以 amd64 映像要在 CI（`ubuntu-latest` 本身是 amd64）原生產生。

---

## 5. 架構審查關卡

需求是「至少確認兩次修改符合軟體設計架構」。這份規劃設了 **三次架構審查 A、B、C，再加一次上線前審查 D**。每次審查都要產出 `docs/reviews/<關卡>-<日期>.md`，**沒有通過不能進入下一個階段**。

### 審查方式

1. Claude 依照下方的檢查清單產出審查報告：每一項標 PASS／FAIL，並附上證據（檔案位置、指令輸出）
2. 開發者逐項確認，有 FAIL 的項目要修正，或寫一份 ADR 說明為什麼接受這個例外
3. （建議）找指導老師或同學做一次 PR review

### 關卡 A：Phase 1 結束

- [ ] 2.1 節的 R1–R6 都已修正，而且各自有回歸測試
- [ ] 分層清楚：Controller 只處理 HTTP，Service 負責業務邏輯，Repository 只負責存取資料；Controller 不直接使用 Repository
- [ ] API 不回傳 JPA Entity，全部都是 DTO；錯誤統一使用 ProblemDetail
- [ ] 每一個帶有資源 ID 的 endpoint 都有擁有權檢查，而且有「別人的 ID → 403」的測試
- [ ] schema 只透過 Flyway 變更；用空的資料庫跑一次 migration 可以成功啟動
- [ ] 長時間的操作都是非同步 job
- [ ] 設定（路徑、密鑰、URL）全部可以用環境變數覆寫，程式裡沒有寫死
- [ ] CI 在測試失敗時會失敗；JaCoCo 覆蓋率不低於 Phase 1 開始時
- [ ] OpenAPI 文件可以存取，而且跟實際行為一致

### 關卡 B：LLM 功能 ①② 完成後

- [ ] D1–D5 的 ADR 已經定案，實作與 ADR 一致
- [ ] 瀏覽器無法直接連到 Python 服務或 Ollama
- [ ] worker 只透過 internal API 存取資料，沒有 DB 連線字串
- [ ] LLM 逾時、輸出格式錯誤、Ollama 停機時，job 會進入 FAILED 並可以重試，不會影響主系統
- [ ] 功能 ② 有評估報告（資料集大小、accuracy、F1、使用的模型和 prompt 版本）
- [ ] 模型與 prompt 版本會跟結果一起存起來（可以追溯是哪個版本產生的）
- [ ] job 管線可以重用：新增一種 task 只需要新增一個模組＋一個 schema

### 關卡 C：LLM 功能 ⑤ 完成後

- [ ] 向量搜尋有做租戶隔離：A 使用者搜不到 B 的專案（有測試）
- [ ] RAG 的回答都有引用來源；找不到依據時有一致的回應
- [ ] 檢索品質有基準數字（recall@10），換模型時可以比較
- [ ] Python 服務的測試覆蓋到 task 層（mock LLM）

### 關卡 D：上線前

- [ ] 映像以非 root 執行；映像中沒有密鑰；容器內不跑 dev server
- [ ] 只有 Ingress 對外；其他服務都是 ClusterIP
- [ ] 有狀態的資料都在 PVC 或受管服務上；刪掉 Pod 不會遺失資料
- [ ] 資料庫有備份和還原的流程，而且實際演練過一次
- [ ] 有 TLS；Secret 不在 git 裡
- [ ] 預設管理員帳號不存在（R3）

---

## 6. 工程慣例

### 架構決策紀錄（ADR）

- 放在 `docs/adr/NNNN-<標題>.md`，格式：背景 → 決策 → 其他選項 → 後果
- 本文件 §1 的 D1–D6，以及 P3-12 的 AWS 方案，都要各寫一份
- 改變一個已經定案的決策時，新增一份 ADR 把舊的取代掉，不要直接修改舊的

### 與 `/ship` 多 agent 流程的關係

- 本文件中的 `P*-NN` 可以直接當作 `/ship P*-NN` 的輸入，總指揮會為它開 issue。一個 issue 對應一個 PR（≤3 個檔案），放不下的條目（例如 P1-07）先拆成多個 issue，各跑一次 `/ship`
- `spec-writer` 會讀 issue 與本文件中對應的條目，產出子任務與驗收條目，由總指揮貼成 issue 的「📋 驗收標準」留言
- 架構審查關卡是 `/ship` 之外的獨立步驟，要人工確認

### 分支與 commit

- 每個 issue 一個分支：`<type>/<issue 編號>-<英文-slug>`，例如 `chore/21-ship-github-flow`（與 `.claude/skills/ship/SKILL.md` 一致）
- commit 遵循現有的 Conventional Commits 風格（`feat:`、`fix:`、`chore:`）

---

## 7. 風險與待決事項

| 項目 | 說明 | 什麼時候決定 |
|---|---|---|
| 研究需求 | 功能 ② 的分類標籤要不要配合國科會計畫的研究問題？ | P2-06 之前跟指導老師確認 |
| 實驗室主機的可用時間 | 5080 是否全天開機、是否跟其他人共用 | P2-04 之前 |
| 現有資料的遷移 | P1-07 會改變專案的識別方式。**已決定**：清掉舊分析資料、重新分析，不遷移（詳見 [ADR 0001](adr/0001-project-identity.md)） | 已決定（2026-11-03） |
| Token 存放位置 | 現在放在 localStorage（容易受到 XSS 影響）；改成 httpOnly cookie 要另外處理 CSRF | P1-16 |
| 套件名稱的拼字錯誤 | `codemangesystem`、`com.example` 是否要更正 | 影響範圍很大，建議放在 Phase 1 最後，或者不改 |
