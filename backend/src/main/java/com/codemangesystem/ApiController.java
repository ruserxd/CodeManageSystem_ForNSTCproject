package com.codemangesystem;

import com.codemangesystem.git_process.model.database.Project;
import com.codemangesystem.git_process.model.git.GitResult;
import com.codemangesystem.git_process.model.git.RepositoryINFO;
import com.codemangesystem.git_process.service.DataBaseService;
import com.codemangesystem.git_process.service.GitCloner;
import com.codemangesystem.git_process.service.GitPuller;
import com.codemangesystem.login_process.model.response.LoginINFO;
import com.codemangesystem.login_process.model.response.SessionResponse;
import com.codemangesystem.login_process.model.user.MyUser;
import com.codemangesystem.login_process.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * api 主要位置
 */
@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class ApiController {
    private final GitCloner gitCloner;
    private final DataBaseService dataBaseService;
    private final UserService userService;
    private final GitPuller gitPuller;
    private static final String LOCAL_BASE_PATH = "src/cloneCode/";

    public ApiController(GitCloner gitCloner, DataBaseService dataBaseService, UserService userService, GitPuller gitPuller) {
        this.gitCloner = gitCloner;
        this.dataBaseService = dataBaseService;
        this.userService = userService;
        this.gitPuller = gitPuller;
    }

    /* Git 資料處理
     * 負責 clone 存儲庫的 api
     * 並將資料做分類存入資料庫前端沒有輸入的話，commId 預設為 HEAD*/
    @PostMapping("/fetch-repo")
    public ResponseEntity<?> fetchRepository(@RequestParam("url") String url,
                                             @RequestParam("commitId") String commitId,
                                             @RequestParam("userId") String userId) {
        try {
            log.info("嘗試抓取 url: {} commitId: {} 的資料", url, commitId);
            log.info("UserId {}", userId);
            return ResponseEntity.ok(gitCloner.cloneRepository(url, commitId, Long.valueOf(userId)));
        } catch (GitAPIException | IOException e) {
            log.error("Error cloning or accessing repository: ", e);
            return ResponseEntity.status(500)
                                 .body("clone 或存取儲存庫時發生錯誤。請檢查 URL 是否正確。");
        }
    }

    /**
     * 獲取 User 目前擁有的資料
     */
    @GetMapping("/getProjectNames")
    public ResponseEntity<?> getProjectNames(@RequestParam("userId") String userId) {
        try {
            log.info("嘗試獲取 userId:{} 的所有 ProjectName", userId);
            List<String> listNames = dataBaseService.getUserProjects(userId);

            log.info("目前有 {}", userId);
            for (String listName : listNames) {
                log.info("Name: {}", listName);
            }

            return new ResponseEntity<>(listNames, HttpStatus.OK);
        } finally {
            log.info("結束輸出 projectNames");
        }
    }

    /**
     * 對當前的資料庫進行 pull
     */
    @GetMapping("/pullProject")
    public ResponseEntity<?> pullByProjectName(@RequestParam("projectName") String projectName) {
        RepositoryINFO info = RepositoryINFO.builder()
                                            .repoName(projectName)
                                            .localPath(LOCAL_BASE_PATH + projectName)
                                            .build();
        GitResult gitResult = gitPuller.pullLocalRepository(info);
        return new ResponseEntity<>(gitResult, HttpStatus.OK);
    }

    /**
     * 透過 ProjectName 獲取 Project 資料
     */
    @PostMapping("/getData")
    public ResponseEntity<Project> getFileDataByProjectName(@RequestParam("ProjectName") String projectName) {
        log.info("嘗試抓取 Data by {}", projectName);
        Project project = dataBaseService.getProjectByProjectName(projectName);
        log.info("getData \n {}", project);
        return new ResponseEntity<>(project, HttpStatus.OK);
    }

    /**
     * 刪除 by ProjectName 的資料
     */
    @GetMapping("/deleteData")
    public String deleteDataByProjectName(@RequestParam("projectName") String projectName, @RequestParam("userId") String userId) {
        return dataBaseService.deleteDataByProjectName(projectName, userId);
    }

    /* 使用者系統 */
    /* 登入 api */
    @PostMapping("/login")
    public ResponseEntity<SessionResponse> login(@RequestBody LoginINFO loginINFO) {
        log.info("嘗試登入 使用者: {}", loginINFO);
        SessionResponse sessionResponse = userService.checkUser(loginINFO);
        return new ResponseEntity<>(sessionResponse, HttpStatus.OK);
    }

    /**
     * 註冊
     */
    @PostMapping("/register")
    public ResponseEntity<SessionResponse> register(@RequestBody MyUser myUser) {
        log.info("嘗試註冊");
        SessionResponse registerResult = userService.userRegister(myUser);
        return new ResponseEntity<>(registerResult, HttpStatus.OK);
    }

    /**
     * 手動加入
     */
    @GetMapping("/addSuperAccount")
    public void addSuperAccount() {
        log.info("手動加入超級帳號");
        userService.addSuperAccount();
    }

    /**
     * 獲取所有使用者資訊
     */
    @GetMapping("/getAllUsers")
    public ResponseEntity<List<MyUser>> getAllUsers() {
        log.info("取得所有使用者的 ID 與帳號");
        List<MyUser> users = userService.getIdsAccounts();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    /**
     * 透過 userId 刪除指定用戶
     */
    @PostMapping("/deleteUser")
    public ResponseEntity<Boolean> deleteUser(@RequestParam("userId")int userId) {
        log.info("刪除 userId 為 {} 的使用者", userId);
        Boolean deleted = true;
        log.info("刪除{}", (deleted) ? "成功" : "失敗");
        return new ResponseEntity<>(deleted, HttpStatus.OK);
    }
}
