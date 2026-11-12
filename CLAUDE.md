# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 專案概述

追蹤 Java 程式碼如何隨時間變化的工具，為國科會研究計畫所開發。使用者送出一個 Git repo URL，後端會 clone 下來、走過 commit 歷史、用 JavaParser 把每個 `.java` 檔拆成方法、比對相鄰 commit 之間每個方法的差異，再把每個方法的歷史存進 MySQL。React 前端則以方法為單位呈現這些歷史。程式碼註解、log 訊息、API 錯誤字串大多是繁體中文。

Monorepo 結構：`backend/`（Spring Boot 3.4、Java 21、Maven）、`frontend/`（React 18 + Vite + antd、Yarn 4）、`nginx/` + `docker-compose.yml`（反向代理、前端、後端、MySQL 8）。

## 指令

### 後端（在 `backend/` 下執行）
```bash
./mvnw spring-boot:run                 # 需要環境變數，見下方
./mvnw test                            # 全部測試（JaCoCo 報告在 target/site/jacoco）
./mvnw test -Dtest=GitDiffAnalyzerTest # 單一測試類別
./mvnw test -Dtest=GitDiffAnalyzerTest#methodName
./mvnw package -DskipTests             # 產生 target/CodeManageSystem-0.0.1-SNAPSHOT.jar（Docker image 會複製這個 jar）
./mvnw pmd:check                       # 依 src/main/resources/pmd/pmd-ruleset.xml 跑 PMD
```
- **測試失敗會讓建置失敗。** pom 已移除 `testFailureIgnore`（#4），有測試失敗時 `mvn test` 會顯示 BUILD FAILURE。但只看 BUILD SUCCESS 仍不夠：要讀輸出中的 `Tests run / Failures / Errors` 統計，確認測試真的有跑、沒有被略過。
- 必要環境變數：`SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`（見 `.env.example`），以及 `JWT_SECRET`（至少 32 bytes，太短的話 `SecurityConfig` 會在啟動時直接失敗）。README 說明是透過 IntelliJ 的 EnvFile 外掛載入 `.env`。
- 測試大多是 Mockito 單元測試。`ApiControllerTest` 是 `@WebMvcTest`，會 import `SecurityConfig` 並自行設定 `app.jwt.secret`。測試用的輸入資料放在 `src/test/resources/`（`classData`、`compareTwoContentData` 等），由測試原始碼中的 `FileReader` 讀取。
- CI（`.github/workflows/backend_test.yml`）在 push 到 `main` 時，以 JDK 21 搭配 MySQL 8 service 執行 `mvn test`。
- 程式碼風格：Google Java Style（`src/main/resources/codestyle/intellij-java-google-style.xml`，縮排 2 格）。命名規則見 `docs/style_guide.md`。

### 前端（在 `frontend/` 下執行）
```bash
yarn install
yarn dev        # Vite 開發伺服器，port 3000
yarn build
```
沒有測試或 lint 的 script。有安裝 Prettier，程式碼使用 tab 縮排。

### Docker
先在 `backend/` 執行 `./mvnw package`，再到 repo 根目錄執行 `docker-compose build && docker-compose up`（使用 `.env` 中的 `DOCKER_*` 變數）。

## 架構

### Port
後端預設 **8080**，可用環境變數調整：`application.properties` 的 `server.port=${SERVER_PORT:8080}`，前端 axios 的 `baseURL` 讀 `VITE_API_URL`（`frontend/src/api/axiosConfig.js`，預設 `http://localhost:8080`；這是瀏覽器要連的位址，不能填 Docker 內部的 `backend:8080`）。後端 Dockerfile 的 `EXPOSE` 和 nginx 的 `/api/` 代理仍寫死 8080，改 port 時要一起改。前端開發伺服器是 3000，CORS 只允許 `http://localhost:3000`。

### 後端套件（`backend/src/main/java/com/codemangesystem/`）
- `ApiController` 是唯一的 REST controller，所有 endpoint 都在 `/api` 底下。
- `git_process/`：核心處理流程。
  - `GitCloner.cloneRepository` 會 clone 到 `src/cloneCode/<repoName>`；若本地已存在則改為 pull 並更新，接著交給 `GitDiffAnalyzer`。`temporaryCloneRepository` 做同樣的分析，但直接回傳 `Project`、不存進資料庫。
  - `GitDiffAnalyzer` 從 HEAD 往回逐對比較 commit（HEAD↔HEAD~1, ...）。`getMethodByContent` 用 JavaParser 抽出方法，`compareTwoContent` 和 `generateGitDiff` 用 java-diff-utils，結果是一棵 `DiffInfo` 資料樹。`analyzePartCommits` 只處理比已存 head SHA 更新的 commit（pull 之後使用）。
  - `GitPuller` 對已存在的本地 clone 做 pull，並以增量方式重新分析。
  - `DataBaseService` 負責讀取、刪除、擁有權檢查（`isProjectOwner`、`isMethodOwner`），並建立延遲載入用的 DTO（`ProjectStructureDTO` → `FileStructureDTO` → `MethodStructureDTO`）。
  - Entity 結構：`Project` → `Files` → `Method` → `DiffInfo`（cascade/orphanRemoval、LAZY fetch、`open-in-view=false`）。`PersonalINFO` 連接 `MyUser` ↔ `Project`（記錄誰擁有哪個專案）。
  - 前端先載入結構（`/getProjectStructure`），再依需求抓單一方法的 diff（`/getDiffInfoByMethod`），所以不會一次拉整個 `Project` 物件圖。
- `login_process/`：`UserService`（註冊／登入，透過 `PasswordBcrypt` 使用 BCrypt）、`JwtService`（發放 HS256 token：`sub`=userId、`role` claim=`USER`/`ADMIN`、8 小時到期）。
- `config/SecurityConfig`：無狀態的 OAuth2 resource-server JWT 驗證。`/api/login`、`/api/register` 公開；`/api/getAllUsers`、`/api/deleteUser`、`/api/addSuperAccount` 與 `/actuator/**` 需要 `ROLE_ADMIN`；其他 endpoint 都需要有效 token。
  - **使用者 ID 一律從 JWT 取得**（`@AuthenticationPrincipal Jwt jwt` → `jwt.getSubject()`），不可從請求參數取得。
  - 接收 `projectName`/`methodId` 的 endpoint，必須先透過 `DataBaseService` 檢查擁有權再做任何事，這同時也防止路徑穿越到 `src/cloneCode/`。
- `config/MyPostConstruct` 在啟動時執行，若超級帳號（帳號 `"123"`）不存在就建立。清除 clone 資料與資料庫的程式碼還在，但已註解掉。

### Clone 下來的 repo
`backend/src/cloneCode/` 存放系統執行時 clone 的 repo（除了 `.gitkeep` 外都被 gitignore）。裡面可能有上千個無關的檔案，例如完整的 spring-boot。**搜尋、grep、重構本專案程式碼時請排除這個資料夾。** 此路徑是相對路徑，所以後端必須以 `backend/` 作為工作目錄啟動。

### 前端（`frontend/src/`）
- `api/axiosConfig.js` 是共用的 axios instance：從 `localStorage['token']` 讀取 JWT 並加上 Bearer header；遇到任何 401 會清除 token 與 `user` cookie，並導回 `/Login`。
- 登入狀態存在 `user` cookie（`react-cookie`），在 `App.jsx` 中處理。路由：`/Login`、`/Register`、`/UserPage`、`/ShowMethodDiff/*`、`/AdminPage`。
- 方法 diff 的檢視元件在 `components/diff/`（`ShowMethodDiff`、`DiffCard`、`HighlightedCode`）。
- Vite alias：`@` → `src/`。

## 我的目標
我正在用這個專案學習 Docker 和 K8S。
- 跟 Dockerfile、compose、K8S YAML 有關的改動：先解釋觀念，讓我自己寫，你再 review
- 每次修改請說明「為什麼這樣做」以及「其他可行做法」
- 不要一次修改超過 3 個檔案，除非我同意

## 技術棧
- 前端：React + Vite + Yarn 4
- 後端：Spring Boot 3.4 / Java 21 / MySQL 8
- LLM 服務：Python FastAPI（新增中）
- 開發機：MacBook Pro M4（arm64）