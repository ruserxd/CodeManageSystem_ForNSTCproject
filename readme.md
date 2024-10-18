# CodeMangeSystem_ForNSTCproject

[You can see hackmd in here](https://hackmd.io/@ruserxd/rJGP1-6RR)

# 目錄
- [配置](#使用此項專案的設置)
    - [Frotend](#Frontend)
    - [Backend](#Backend)
- [Git Process](#GitProcess)
    - [Git Cloner](#GitCloner)

# 使用此項專案的設置

## 個人使用工具
- macbook air m2
- vscode
- IntelliJ IDEA 2022.2.5 (Ultimate Edition)

## Frontend
在開始設置本專案之前，請確保您的本地環境已經安裝以下工具：
- React
- yarn
- ngrok

請先確保以上的都有事先安裝於 local

### 概念
React 當作我們的前端框架

yarn 當作我們的依賴管理工具

ngrok 負責讓後端為公開伺服器

---
開始步驟:
1. 打開電腦上的 terminal
    ```
    ngrok http http://localhost:8080
    ```
2. 開啟伺服器，將 ngrok 獲得的網址修改於 api/axiosConfig.js 的 baseURL

3. cd 到 frontend 的資料夾，安裝配置
    ```
    yarn install
    ```
4. `yarn start` 即可開啟網頁

---

## Backend

### 概念
Spring Boot 當作後端的框架

SQl 當作系統的資料庫

env 管理環境設定

有寫一個 .env.example

請將開設好的 sql 的資訊 (urlname, username, passwrod) 填入並創立一個 .env 檔案

記得寫好 env 將 Run Configurations 的 Enable EnvFile 設置打開
![pic](https://hackmd.io/_uploads/SyQGmpnyJx.png =80%x)


## GitProcess

### GitCloner
固定格式
- clone 下來的檔案存放位置
    ```java
    private static final String CLONE_LOCAL_BASE_PATH = "src/cloneCode/";
    ```
- clone 的分支
    ```java
    private static final String DEFAULT_BRANCH = "main";
    ```

1. CloneRepository
- 目的 : 負責將資料 clone 下來
- 操作 :
   ```mermaid
   graph TD;
   clone_資料 --> 本地端無資料_執行clone;
   clone_資料 --> 本地端有資料_執行pull;
   本地端無資料_執行clone--> clone資料到本地端;
   本地端有資料_執行pull --> pull本地端資料
   clone資料到本地端 --> 將資料分類存入SQL;
   pull本地端資料 --> 透過GitTree更新SQL內容;
   ```

2. getRepoNameFromUrl
- 目的 : 獲取檔案的名稱
- 操作 : 將網址最後的 '/' 後面 GitRepository 當作當作 local 資料夾名稱

3. isRepositoryClonedLocally
- 目的 : 判斷 local 是否存在此儲存庫
- 操作 : git init 時，我們其實就會在資料夾內新增一個 .git 檔案      , clone 下來時我們也會有相對應的 .git 檔案

4. renewRepositoryLocally
- 目的 : pull 更新本地端資料
- 操作 : Git 物件打開本地端資料，對 main 分支執行 pull

### GitDiffAnalyzer
```mermaid
gitGraph
       commit
       commit
       commit
       commit
```
目前的做法是先將 3(Head) 的部分 clone 下來，接著一個一個去比對

ex. 3 <-> 2, 2 <-> 1, 1 <-> 0



示意圖 (a 代表較新的版本, b 代表較舊的版本)
```mermaid
graph TD
id1[clone_Head_的資料] --> id2[對各個 commit 進行操作];
id2[對各個 commit 進行操作] --> id3[透過_JavaParser_分類出方法];
id3[透過_JavaParser_分類出方法] --> id4[JavaDiff_比較兩者差異];
id4[JavaDiff_比較兩者差異] -- a無方法_b有 --> id5[代表刪減--];
id4[JavaDiff_比較兩者差異] -- a有方法_b有 --> id6[代表有diff];
id4[JavaDiff_比較兩者差異] -- a有方法_b無 --> id7[代表新增++];
```

1. analyzeCommits
- 目的 :
- 操作 :

2. prepareTreeParser
- 目的 :
- 操作 :

3. takeCommitINFO
- 目的 :
- 操作 :

4. getFileContent
- 目的 :
- 操作 :

5. compareTwoContent
- 目的 :
- 操作 :

6. getContentMethod
- 目的 :
- 操作 :

7. generateLikeGitDiff
- 目的 :
- 操作 :

8. addDiffInfoInToProject
- 目的 :
- 操作 :


## 許可證

本專案採用 MIT 許可證。詳情請見 [LICENSE](LICENSE) 檔案。
