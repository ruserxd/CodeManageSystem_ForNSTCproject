package com.codemangesystem;

import com.codemangesystem.gitProcess.model_DataBase.Project;
import com.codemangesystem.gitProcess.model_Git.GitResult;
import com.codemangesystem.gitProcess.model_Repo.RepositoryINFO;
import com.codemangesystem.gitProcess.service.GetDataBse;
import com.codemangesystem.gitProcess.service.GitCloner;
import com.codemangesystem.gitProcess.service.GitPuller;
import com.codemangesystem.loginProcess.model_response.LoginINFO;
import com.codemangesystem.loginProcess.model_response.sessionResponse;
import com.codemangesystem.loginProcess.model_user.MyUser;
import com.codemangesystem.loginProcess.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api")
public class ApiController {

    private final GitCloner gitCloner;
    private final GetDataBse getDataBse;
    private final UserService userService;
    private final GitPuller gitPuller;

    public ApiController(GitCloner gitCloner, GetDataBse getDataBse, UserService userService, GitPuller gitPuller) {
        this.gitCloner = gitCloner;
        this.getDataBse = getDataBse;
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
            List<String> listNames = getDataBse.getUserProjects(userId);

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
    private static final String LOCAL_BASE_PATH = "src/cloneCode/";

    @GetMapping("/pullProject")
    public ResponseEntity<?> pullByProjectName(@RequestParam("projectName") String projectName) {
        RepositoryINFO info = RepositoryINFO.builder()
                                            .repoName(projectName)
                                            .localPath(LOCAL_BASE_PATH + projectName)
                                            .build();
        GitResult gitResult = gitPuller.pullLocalRepository(info);
        return new ResponseEntity<>(gitResult, HttpStatus.OK);
    }

    // TODO: 修改成針對使用者去做獲取資料
    /**
     * 透過 ProjectName 獲取 Project 資料
     */
    @PostMapping("/getData")
    public ResponseEntity<Project> getFileDataByProjectName(@RequestParam("ProjectName") String projectName) {
        log.info("嘗試抓取 Data by {}", projectName);
        Project project = getDataBse.getProjectByProjectName(projectName);
        log.info("getData \n {}", project);
        return new ResponseEntity<>(project, HttpStatus.OK);
    }

    /**
     * 刪除 by ProjectName 的資料
     */
    @GetMapping("/deleteData")
    public String deleteDataByProjectName(@RequestParam("projectName") String projectName, @RequestParam("userId") String userId) {
        return getDataBse.deleteDataByProjectName(projectName, userId);
    }

    /* 登入系統 */
    /* 登入 api */
    @PostMapping("/login")
    public ResponseEntity<sessionResponse> login(@RequestBody LoginINFO loginINFO) {
        log.info("嘗試登入 使用者: {}", loginINFO);
        sessionResponse sessionResponse = userService.checkUser(loginINFO);
        return new ResponseEntity<>(sessionResponse, HttpStatus.OK);
    }

    /**
     * 註冊
     */
    @PostMapping("/register")
    public ResponseEntity<sessionResponse> register(@RequestBody MyUser myUser) {
        log.info("嘗試註冊");
        sessionResponse registerResult = userService.userRegister(myUser);
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
}
