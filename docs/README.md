# CodeMangeSystem_ForNSTCproject

# 目錄
- [配置](#使用此項專案的設置)
    - [Frotend](#Frontend)
    - [Backend](#Backend)
- [Git Process](#GitProcess)
    - [Git Cloner](#GitCloner)

# 使用此項專案的設置

## TODO
- 標註父類別的修改，影響哪些子類別的 function

## 個人使用工具
- macbook pro m4

## Docker 一鍵配置
1. 依照目錄下的 .env.example 的要求，填入資料庫的相關 URL, USERNAME, PASSWORD
2. Backend 進行 Maven 打包操作 (日後也加入 Docker 中)
3. 在目錄下依序執行 
    - docker-compose build
    - docker-compose up

## Frontend
在開始設置本專案之前，請確保您的本地環境已經安裝以下工具：
- React
- yarn

### 概念
React 當作我們的前端框架

yarn 當作前端依賴管理工具

vite 負責啟動開發伺服器

## Backend

### 概念
Spring Boot 當作後端的框架

SQl 當作系統的資料庫

env 管理環境設定

記得寫好 env 將 Run Configurations 的 Enable EnvFile 設置打開
![pic](https://hackmd.io/_uploads/SyQGmpnyJx.png =80%x)

## GitProcess

### GitCloner
Clone 的示意圖

```mermaid
graph TD;
clone_資料 --> 本地端無資料_執行clone;
clone_資料 --> 本地端有資料_執行pull;
本地端無資料_執行clone--> clone資料到本地端;
本地端有資料_執行pull --> pull本地端資料
clone資料到本地端 --> 將資料分類存入SQL;
pull本地端資料 --> 透過GitTree更新SQL內容;
   ```

### GitDiffAnalyzer

Git 的歷程示意圖
```mermaid
gitGraph
       commit
       commit
       commit
       commit
```
目前的做法是先將 3(Head) 的部分 clone 下來，接著一個一個去比對

ex. 3 <-> 2, 2 <-> 1, 1 <-> 0

分析示意圖 (a 代表較新的版本, b 代表較舊的版本)
```mermaid
graph TD
id1[clone_Head_的資料] --> id2[對各個 commit 進行操作];
id2[對各個 commit 進行操作] --> id3[透過_JavaParser_分類出方法];
id3[透過_JavaParser_分類出方法] --> id4[JavaDiff_比較兩者差異];
id4[JavaDiff_比較兩者差異] -- a無方法_b有 --> id5[代表刪減--];
id4[JavaDiff_比較兩者差異] -- a有方法_b有 --> id6[代表有diff];
id4[JavaDiff_比較兩者差異] -- a有方法_b無 --> id7[代表新增++];
```


## 許可證

本專案採用 MIT 許可證。詳情請見 [LICENSE](LICENSE) 檔案。

## 致謝 & 參與
1. 國科會編號:NSTC 113-2813-C-035-131-E 研究計畫之補助
2. 2025 台灣軟體工程研討會展示論文
