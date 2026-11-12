package com.codemangesystem;

import com.codemangesystem.git_process.model.database.DiffInfo;
import com.codemangesystem.git_process.model.database.Project;
import com.codemangesystem.git_process.model.dto.ProjectStructureDTO;
import com.codemangesystem.git_process.model.git.GitResult;
import com.codemangesystem.git_process.model.git.RepositoryINFO;
import com.codemangesystem.git_process.service.DataBaseService;
import com.codemangesystem.git_process.service.GitCloner;
import com.codemangesystem.git_process.service.GitPuller;
import com.codemangesystem.login_process.model.response.LoginINFO;
import com.codemangesystem.login_process.model.response.SessionResponse;
import com.codemangesystem.login_process.model.user.MyUser;
import com.codemangesystem.login_process.service.JwtService;
import com.codemangesystem.login_process.service.UserService;
import com.codemangesystem.quota.QuotaService;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * api 主要位置
 * 除了 login、register 外都需要帶 JWT，使用者身分一律從 token 取得，不採用前端傳入的 userId
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

  private final GitCloner gitCloner;
  private final DataBaseService dataBaseService;
  private final UserService userService;
  private final GitPuller gitPuller;
  private final JwtService jwtService;
  private final QuotaService quotaService;
  private static final String LOCAL_BASE_PATH = "src/cloneCode/";

  public ApiController(GitCloner gitCloner, DataBaseService dataBaseService,
      UserService userService, GitPuller gitPuller, JwtService jwtService,
      QuotaService quotaService) {
    this.gitCloner = gitCloner;
    this.dataBaseService = dataBaseService;
    this.userService = userService;
    this.gitPuller = gitPuller;
    this.jwtService = jwtService;
    this.quotaService = quotaService;
  }

  /**
   * 扣一次今日「分析」額度（以嘗試次數計，失敗也算）。ADMIN 不受限也不消耗。
   *
   * @return 額度已用完時回傳 429 的 response；可繼續處理時回傳 null
   */
  private ResponseEntity<String> rejectIfAnalysisQuotaExceeded(Jwt jwt) {
    if ("ADMIN".equals(jwt.getClaimAsString("role"))) {
      return null;
    }
    if (quotaService.tryAcquire(QuotaService.Kind.ANALYSIS)) {
      return null;
    }
    log.warn("今日分析額度已用完，userId: {}", jwt.getSubject());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body("今日分析額度已用完，請明日再試");
  }

  /**
   * 從 JWT 取得目前登入的 userId
   */
  private static Long currentUserId(Jwt jwt) {
    return Long.valueOf(jwt.getSubject());
  }

  /**
   * 臨時分析 Git 專案，不存入資料庫直接回傳結果
   */
  @PostMapping("/get-method-diff-temporary")
  public ResponseEntity<?> getMethodDiffTemporary(
      @RequestParam("url") String url,
      @RequestParam("commitId") String commitId,
      @AuthenticationPrincipal Jwt jwt) {
    ResponseEntity<String> rejected = rejectIfAnalysisQuotaExceeded(jwt);
    if (rejected != null) {
      return rejected;
    }
    try {
      log.info("臨時分析 URL: {} CommitId: {}", url, commitId);

      // 使用臨時分析服務
      Project tempProject = gitCloner.temporaryCloneRepository(url, commitId);

      log.info("臨時分析完成，回傳結果");
      return ResponseEntity.ok(tempProject);

    } catch (IllegalArgumentException e) {
      // repo URL 未通過驗證（GitFunction.validateRepoUrl），屬於使用者輸入錯誤
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (GitAPIException | IOException e) {
      log.error("臨時分析時發生錯誤: ", e);
      return ResponseEntity.status(500)
          .body("臨時分析專案時發生錯誤。請檢查 URL 是否正確。");
    }
  }

  /*
   * Git 資料處理
   * 負責 clone 存儲庫的 api
   * 並將資料做分類存入資料庫前端沒有輸入的話，commId 預設為 HEAD
   */
  @PostMapping("/fetch-repo")
  public ResponseEntity<?> fetchRepository(@RequestParam("url") String url,
      @RequestParam("commitId") String commitId,
      @AuthenticationPrincipal Jwt jwt) {
    Long userId = currentUserId(jwt);
    ResponseEntity<String> rejected = rejectIfAnalysisQuotaExceeded(jwt);
    if (rejected != null) {
      return rejected;
    }
    try {
      log.info("嘗試抓取 url: {} commitId: {} 的資料", url, commitId);
      log.info("UserId {}", userId);
      return ResponseEntity.ok(gitCloner.cloneRepository(url, commitId, userId));
    } catch (IllegalArgumentException e) {
      // repo URL 未通過驗證（GitFunction.validateRepoUrl），屬於使用者輸入錯誤
      return ResponseEntity.badRequest().body(e.getMessage());
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
  public ResponseEntity<?> getProjectNames(@AuthenticationPrincipal Jwt jwt) {
    String userId = jwt.getSubject();
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
  public ResponseEntity<?> pullByProjectName(@RequestParam("projectName") String projectName,
      @AuthenticationPrincipal Jwt jwt) {
    // 只能 pull 自己擁有的專案，同時避免 projectName 被用來指向其他路徑
    if (!dataBaseService.isProjectOwner(currentUserId(jwt), projectName)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    ResponseEntity<String> rejected = rejectIfAnalysisQuotaExceeded(jwt);
    if (rejected != null) {
      return rejected;
    }
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
  public ResponseEntity<Project> getFileDataByProjectName(
      @RequestParam("ProjectName") String projectName, @AuthenticationPrincipal Jwt jwt) {
    if (!dataBaseService.isProjectOwner(currentUserId(jwt), projectName)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    log.info("嘗試抓取 Data by {}", projectName);
    Project project = dataBaseService.getProjectByProjectName(projectName);
    log.info("getData \n {}", project);
    return new ResponseEntity<>(project, HttpStatus.OK);
  }

  /**
   * 刪除 by ProjectName 的資料
   */
  @GetMapping("/deleteData")
  public String deleteDataByProjectName(@RequestParam("projectName") String projectName,
      @AuthenticationPrincipal Jwt jwt) {
    return dataBaseService.deleteDataByProjectName(projectName, jwt.getSubject());
  }

  /**
   * 獲取專案結構（不包含 DiffInfo）
   */
  @GetMapping("/getProjectStructure")
  public ResponseEntity<ProjectStructureDTO> getProjectStructure(
      @RequestParam("projectName") String projectName, @AuthenticationPrincipal Jwt jwt) {
    if (!dataBaseService.isProjectOwner(currentUserId(jwt), projectName)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    log.info("獲取專案結構: {}", projectName);
    ProjectStructureDTO structure = dataBaseService.getProjectStructure(projectName);
    if (structure == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(structure);
  }

  /**
   * 根據 methodId 獲取 DiffInfo 列表
   */
  @GetMapping("/getDiffInfoByMethod")
  public ResponseEntity<List<DiffInfo>> getDiffInfoByMethod(
      @RequestParam("methodId") Long methodId, @AuthenticationPrincipal Jwt jwt) {
    if (!dataBaseService.isMethodOwner(currentUserId(jwt), methodId)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    log.info("獲取 Method {} 的差異信息", methodId);
    List<DiffInfo> diffInfoList = dataBaseService.getDiffInfoByMethodId(methodId);
    return ResponseEntity.ok(diffInfoList);
  }

  /* 使用者系統 */
  /* 登入 api */
  @PostMapping("/login")
  public ResponseEntity<SessionResponse> login(@RequestBody LoginINFO loginINFO) {
    log.info("嘗試登入{}", loginINFO.getUserAccount());
    SessionResponse sessionResponse = userService.checkUser(loginINFO);
    if (sessionResponse.isSuccess()) {
      sessionResponse.setToken(jwtService.generateToken(sessionResponse.getMyUser()));
    }
    return new ResponseEntity<>(sessionResponse, HttpStatus.OK);
  }

  /**
   * 訪客登入：不需帳密，demo 帳號只來自 app.demo.account 設定
   */
  @PostMapping("/demo-login")
  public ResponseEntity<SessionResponse> demoLogin() {
    log.info("嘗試訪客登入");
    SessionResponse sessionResponse = userService.demoLogin();
    if (sessionResponse.isSuccess()) {
      sessionResponse.setToken(jwtService.generateToken(sessionResponse.getMyUser()));
    }
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
   * 手動加入（僅 ADMIN）
   */
  @GetMapping("/addSuperAccount")
  public void addSuperAccount() {
    log.info("手動加入超級帳號");
    userService.addSuperAccount();
  }

  /**
   * 獲取所有使用者資訊（僅 ADMIN）
   */
  @GetMapping("/getAllUsers")
  public ResponseEntity<List<MyUser>> getAllUsers() {
    log.info("取得所有使用者的 ID 與帳號");
    List<MyUser> users = userService.getIdsAccounts();
    return new ResponseEntity<>(users, HttpStatus.OK);
  }

  /**
   * 透過 userId 刪除指定用戶（僅 ADMIN）
   */
  @PostMapping("/deleteUser")
  public ResponseEntity<Boolean> deleteUser(@RequestParam("userId") Long userId) {
    log.info("刪除 userId 為 {} 的使用者", userId);
    Boolean deleted = userService.deleteUserById(userId);
    log.info("刪除{}", (deleted) ? "成功" : "失敗");
    return new ResponseEntity<>(deleted, HttpStatus.OK);
  }
}
