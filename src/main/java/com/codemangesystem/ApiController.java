package com.codemangesystem;

import com.codemangesystem.gitProcess.model_Data.Files;
import com.codemangesystem.gitProcess.service.GetDataBse;
import com.codemangesystem.gitProcess.service.GitCloner;
import com.codemangesystem.gitProcess.service.GitDiffAnalyzer;
import com.codemangesystem.loginProcess.model_response.LoginINFO;
import com.codemangesystem.loginProcess.model_response.LoginResponse;
import com.codemangesystem.loginProcess.model_response.RegisterResponse;
import com.codemangesystem.loginProcess.model_user.MyUser;
import com.codemangesystem.loginProcess.services.MyUserService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final GitDiffAnalyzer gitDiffAnalyzer;
    private final GetDataBse getDataBse;
    private final MyUserService myUserService;

    @Autowired
    public ApiController(GitCloner gitCloner, GitDiffAnalyzer gitDiffAnalyzer, GetDataBse getDataBse
            , MyUserService myUserService) {
        this.gitCloner = gitCloner;
        this.gitDiffAnalyzer = gitDiffAnalyzer;
        this.getDataBse = getDataBse;
        this.myUserService = myUserService;
    }

    /* Git 資料處理 */
    // (前端未使用) 負責分類存儲庫的 api
    @PostMapping("/fetch-repo/categorize")
    public ResponseEntity<List<Files>> categorizeCode(@RequestParam("Path") String path) throws GitAPIException, IOException {
        log.info("嘗試分類 Data by " + path);
        return new ResponseEntity<>(gitDiffAnalyzer.analyzeAllCommits(path), HttpStatus.OK);
    }

    // 負責 clone 存儲庫的 api ，並將資料做分類存入資料庫
    @PostMapping("/fetch-repo")
    public ResponseEntity<?> fetchRepo(@RequestParam("url") String url, @RequestParam("commitId") String commitId) {
        try {
            log.info("嘗試抓取 " + "url: " + url + "commitId: " + commitId + " 的資料");
            return ResponseEntity.ok(gitCloner.cloneRepository(url, commitId));
        } catch (GitAPIException | IOException e) {
            log.error("Error cloning or accessing repository: ", e);
            return ResponseEntity.status(500).body("clone 或存取儲存庫時發生錯誤。請檢查 URL 是否正確。");
        }
    }

    // 獲取目前擁有的資料
    @GetMapping("/getProjectNames")
    public ResponseEntity<?> getProjectIdInDataBase() {
        try {
            List<String> listNames = getDataBse.getAllProjectNames();

            log.info("目前有");
            for (String listName : listNames) {
                log.info("Name: " + listName);
            }

            return new ResponseEntity<>(listNames, HttpStatus.OK);
        } finally {
            log.info("結束輸出 projectNames");
        }
    }

    // 透過 ProjectName 獲取 Files 資料
    @PostMapping("/getData")
    public ResponseEntity<List<Files>> getData(@RequestParam("ProjectName") String projectName) {
        log.info("嘗試抓取 Data by " + projectName);
        return new ResponseEntity<>(getDataBse.getFilesByProjectName(projectName), HttpStatus.OK);
    }

    // 手動刪除資料
    @GetMapping("/deleteData")
    public String deleteData(@RequestParam("ProjectName") String projectName) {
        return getDataBse.deleteData(projectName);
    }

    /* 登入系統 */
    // 登入 api
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginINFO loginINFO) {
        log.info("嘗試登入 使用者: " + loginINFO);
        LoginResponse loginResponse = myUserService.checkUser(loginINFO);
        return new ResponseEntity<>(loginResponse, HttpStatus.OK);
    }

    // 註冊
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody MyUser myUser) {
        log.info("嘗試註冊");
        RegisterResponse registerResult = myUserService.userRegister(myUser);
        return new ResponseEntity<>(registerResult, HttpStatus.OK);
    }

    // 手動加入
    @GetMapping("/addSuperAccount")
    public void addSuperAccount() {
        log.info("手動加入超級帳號");
        myUserService.addSuperAccount();
    }
}
