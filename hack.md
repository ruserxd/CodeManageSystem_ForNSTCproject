# CodeMangeSystem_ForNSTCproject
# 目錄
- [Git Process](#GitProcess)
    - [Git Cloner](#GitCloner)
## GitProcess

### GitCloner
固定格式:
clone 下來的檔案存放位置
```private static final String CLONE_LOCAL_BASE_PATH = "src/cloneCode/";```

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
- 目的 : 負責將資料 clone 下來
- 操作 : 將網址最後的 '/' 後面 GitRepository 當作當作 local 資料夾名稱

3. isRepositoryClonedLocally
- 目的 : 判斷 local 是否存在此儲存庫
- 操作 : git init 時，我們其實就會在資料夾內新增一個 .git 檔案      , clone 下來時我們也會有相對應的 .git 檔案

4. renewRepositoryLocally
- 目的 : pull 更新本地端資料
- 操作 : Git 物件打開本地端資料，對 main 分支執行 pull