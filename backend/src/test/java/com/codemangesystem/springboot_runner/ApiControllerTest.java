package com.codemangesystem.springboot_runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codemangesystem.ApiController;
import com.codemangesystem.config.SecurityConfig;
import com.codemangesystem.git_process.model.database.Project;
import com.codemangesystem.git_process.model.dto.ProjectStructureDTO;
import com.codemangesystem.git_process.model.git.GitResult;
import com.codemangesystem.git_process.model.git.GitStatus;
import com.codemangesystem.git_process.model.git.RepositoryINFO;
import com.codemangesystem.git_process.service.DataBaseService;
import com.codemangesystem.git_process.service.GitCloner;
import com.codemangesystem.git_process.service.GitDiffAnalyzer;
import com.codemangesystem.git_process.service.GitPuller;
import com.codemangesystem.login_process.model.response.LoginINFO;
import com.codemangesystem.login_process.model.response.SessionResponse;
import com.codemangesystem.login_process.model.user.MyUser;
import com.codemangesystem.login_process.model.user.UserAuthority;
import com.codemangesystem.login_process.service.JwtService;
import com.codemangesystem.login_process.service.UserService;
import com.codemangesystem.quota.AnalysisLimits;
import com.codemangesystem.quota.QuotaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ApiController.class)
@Import({SecurityConfig.class, ApiControllerTest.LimitsConfig.class})
@TestPropertySource(properties = "app.jwt.secret=test-secret-test-secret-test-secret-123")
public class ApiControllerTest {

  // userId = 1 的一般使用者
  private static final RequestPostProcessor USER = jwt()
      .jwt(token -> token.subject("1"))
      .authorities(new SimpleGrantedAuthority("ROLE_USER"));

  private static final RequestPostProcessor ADMIN = jwt()
      .jwt(token -> token.subject("99").claim("role", "ADMIN"))
      .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));

  // userId = 2 的唯讀 demo 帳號
  private static final RequestPostProcessor DEMO = jwt()
      .jwt(token -> token.subject("2"))
      .authorities(new SimpleGrantedAuthority("ROLE_DEMO"));

  private static final int USER_MAX = 3;
  private static final int ADMIN_MAX = 100;
  private static final int MAX_PROJECTS = 2;

  /** 用真實的 AnalysisLimits（小上限、磁碟門檻 0），才能驗證槽位與角色邏輯。 */
  @TestConfiguration
  static class LimitsConfig {

    @Bean
    AnalysisLimits analysisLimits() {
      return new AnalysisLimits(USER_MAX, ADMIN_MAX, MAX_PROJECTS, 0);
    }
  }

  // 缺少 role claim 的 JWT
  private static final RequestPostProcessor NO_ROLE = jwt()
      .jwt(token -> token.subject("1"))
      .authorities(new SimpleGrantedAuthority("ROLE_USER"));

  @Autowired
  private AnalysisLimits analysisLimits;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private GitCloner gitCloner;

  @MockitoBean
  private DataBaseService dataBaseService;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private GitPuller gitPuller;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private QuotaService quotaService;

  @Autowired
  private ObjectMapper objectMapper;

  private LoginINFO testLoginInfo;
  private MyUser testUser;
  private SessionResponse testSessionResponse;
  private Project testProject;

  @BeforeEach
  void setUp() {
    // 單例的槽位在每個測試前重設為「未被佔用」
    while (analysisLimits.tryAcquire()) {
      // 取光
    }
    analysisLimits.release();

    // 預設額度未滿
    when(quotaService.tryAcquire(any())).thenReturn(true);

    // 預設 loginINFO
    testLoginInfo = LoginINFO.builder().build();
    testLoginInfo.setUserAccount("testUser");
    testLoginInfo.setUserPassword("testPass");

    // 預設 MyUser
    testUser = new MyUser();
    testUser.setUserAccount("testUser");
    testUser.setUserPassword("testPass");

    // 預設 SessionResponse
    testSessionResponse = SessionResponse.builder()
        .success(true)
        .message("Login successful")
        .myUser(testUser)
        .build();

    // 預設 Project
    testProject = new Project();
    testProject.setProjectName("testProject");
  }

  @Test
  @DisplayName("測試 FetchRepository api")
  public void testFetchRepository() throws Exception {
    // 預設回傳值
    GitResult mockResult = GitResult.builder()
        .status(GitStatus.CLONE_SUCCESS)
        .message("Repository cloned successfully")
        .build();

    // 模擬
    when(gitCloner.cloneRepository(anyString(), anyString(), eq(1L), anyInt()))
        .thenReturn(mockResult);

    // 測試與驗證，前端傳入的 userId 會被忽略，改用 token 內的 userId
    mockMvc.perform(post("/api/fetch-repo")
            .with(USER)
            .param("url", "https://github.com/test/repo")
            .param("commitId", "main")
            .param("userId", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLONE_SUCCESS"));
  }

  @Test
  @DisplayName("測試 fetchRepository api throw 的處理")
  public void testFetchRepositoryThrow() throws Exception {
    when(gitCloner.cloneRepository(anyString(), anyString(), any(Long.class), anyInt()))
        .thenThrow(new GitAPIException("git error") {
        });

    mockMvc.perform(post("/api/fetch-repo")
            .with(USER)
            .param("url", "https://github.com/test/repo")
            .param("commitId", "main"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().string("clone 或存取儲存庫時發生錯誤。請檢查 URL 是否正確。"));
  }

  @Test
  @DisplayName("測試 fetchRepository api 遇到不合法 URL 回 400")
  public void testFetchRepositoryInvalidUrl() throws Exception {
    when(gitCloner.cloneRepository(anyString(), anyString(), any(Long.class), anyInt()))
        .thenThrow(new IllegalArgumentException("只接受 https 開頭的 repo URL"));

    mockMvc.perform(post("/api/fetch-repo")
            .with(USER)
            .param("url", "file:///etc/passwd")
            .param("commitId", "HEAD"))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("只接受 https 開頭的 repo URL"));
  }

  @Test
  @DisplayName("測試臨時分析 api 遇到不合法 URL 回 400")
  public void testTemporaryInvalidUrl() throws Exception {
    when(gitCloner.temporaryCloneRepository(anyString(), anyString(), anyInt()))
        .thenThrow(new IllegalArgumentException("只接受 https 開頭的 repo URL"));

    mockMvc.perform(post("/api/get-method-diff-temporary")
            .with(USER)
            .param("url", "http://10.0.0.1/a/b")
            .param("commitId", "HEAD"))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("只接受 https 開頭的 repo URL"));
  }

  @Test
  @DisplayName("測試 getProjectNames api")
  public void testGetProjectNames() throws Exception {
    List<String> projectNames = Arrays.asList("project1", "project2");
    when(dataBaseService.getUserProjects("1")).thenReturn(projectNames);

    mockMvc.perform(get("/api/getProjectNames")
            .with(USER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").value("project1"))
        .andExpect(jsonPath("$[1]").value("project2"));
  }

  @Test
  @DisplayName("測試 pullProject api")
  public void testPullProject() throws Exception {
    GitResult mockResult = GitResult.builder()
        .status(GitStatus.PULL_SUCCESS)
        .message("Repository pulled successfully")
        .build();

    when(gitPuller.pullLocalRepository(any(RepositoryINFO.class), anyInt()))
        .thenReturn(mockResult);

    when(dataBaseService.isProjectOwner(1L, "testProject")).thenReturn(true);

    mockMvc.perform(get("/api/pullProject")
            .with(USER)
            .param("projectName", "testProject"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PULL_SUCCESS"));
  }

  @Test
  @DisplayName("測試 getFileDataByProjectName api")
  public void testGetFileDataByProjectName() throws Exception {
    when(dataBaseService.getProjectByProjectName(anyString()))
        .thenReturn(testProject);

    when(dataBaseService.isProjectOwner(1L, "testProject")).thenReturn(true);

    mockMvc.perform(post("/api/getData")
            .with(USER)
            .param("ProjectName", "testProject"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.projectName").value("testProject"));
  }

  @Test
  @DisplayName("測試 deleteDataByProjectName api")
  public void testDeleteDataByProjectName() throws Exception {
    when(dataBaseService.deleteDataByProjectName("testProject", "1"))
        .thenReturn("Deleted successfully");

    mockMvc.perform(get("/api/deleteData")
            .with(USER)
            .param("projectName", "testProject")
            .param("userId", "2"))
        .andExpect(status().isOk())
        .andExpect(content().string("Deleted successfully"));
  }

  @Test
  @DisplayName("測試 login api")
  public void testLogin() throws Exception {
    when(userService.checkUser(any(LoginINFO.class)))
        .thenReturn(testSessionResponse);
    when(jwtService.generateToken(testUser)).thenReturn("test-token");

    mockMvc.perform(post("/api/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testLoginInfo)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Login successful"))
        .andExpect(jsonPath("$.token").value("test-token"))
        .andExpect(jsonPath("$.myUser.userAccount").value("testUser"))
        .andExpect(jsonPath("$.myUser.userPassword").doesNotExist());
  }

  @Test
  @DisplayName("測試 demo-login api：免 JWT，成功時回傳 DEMO 的 token")
  public void testDemoLoginSuccess() throws Exception {
    MyUser demoUser = MyUser.builder()
        .userId(2L)
        .userAccount("demo")
        .userPassword("hashed")
        .userAuthority(UserAuthority.DEMO)
        .build();
    SessionResponse demoResponse = SessionResponse.builder()
        .success(true)
        .message("Success")
        .myUser(demoUser)
        .build();
    when(userService.demoLogin()).thenReturn(demoResponse);
    when(jwtService.generateToken(any(MyUser.class))).thenReturn("demo-token");

    // 不帶任何 JWT
    mockMvc.perform(post("/api/demo-login"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.token").value("demo-token"))
        .andExpect(jsonPath("$.myUser.userAccount").value("demo"))
        .andExpect(jsonPath("$.myUser.userPassword").doesNotExist());

    verify(jwtService).generateToken(
        argThat(u -> u.getUserAuthority() == UserAuthority.DEMO));
  }

  @Test
  @DisplayName("測試 demo-login api：未啟用時回傳明確失敗訊息，且不簽 token")
  public void testDemoLoginNotEnabled() throws Exception {
    when(userService.demoLogin()).thenReturn(
        SessionResponse.builder().success(false).message("demo 帳號未啟用").build());

    mockMvc.perform(post("/api/demo-login"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("demo 帳號未啟用"))
        .andExpect(jsonPath("$.token").doesNotExist());

    verify(jwtService, never()).generateToken(any());
  }

  @Test
  @DisplayName("測試 demo-login api：帶任何 JWT 或參數都一樣，不接受指定帳號")
  public void testDemoLoginIgnoresInput() throws Exception {
    when(userService.demoLogin()).thenReturn(
        SessionResponse.builder().success(false).message("demo 帳號未啟用").build());

    mockMvc.perform(post("/api/demo-login").with(USER).param("userAccount", "admin"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false));

    verify(userService).demoLogin();
    verify(userService, never()).checkUser(any());
  }

  @Test
  @DisplayName("測試 register api 仍能接收密碼（userPassword 為 WRITE_ONLY）")
  public void testRegisterAcceptsPassword() throws Exception {
    when(userService.userRegister(any(MyUser.class))).thenReturn(testSessionResponse);

    // 用原始 JSON，因為 objectMapper 序列化 MyUser 時已經不會輸出密碼
    mockMvc.perform(post("/api/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userAccount\":\"newUser\",\"userPassword\":\"secret\"}"))
        .andExpect(status().isOk());

    verify(userService).userRegister(argThat(u -> "secret".equals(u.getUserPassword())));
  }

  @Test
  @DisplayName("測試 getAllUsers api 不回傳密碼雜湊")
  public void testGetAllUsersHidesPassword() throws Exception {
    MyUser user = MyUser.builder().userId(1L).userAccount("a").userPassword("$2a$hash").build();
    when(userService.getIdsAccounts()).thenReturn(List.of(user));

    mockMvc.perform(get("/api/getAllUsers").with(ADMIN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].userAccount").value("a"))
        .andExpect(jsonPath("$[0].userPassword").doesNotExist());
  }

  @Test
  @DisplayName("MyUser.toString() 不包含密碼，避免寫進 log")
  public void testMyUserToStringHidesPassword() {
    MyUser user = MyUser.builder().userAccount("a").userPassword("$2a$hash").build();
    assertFalse(user.toString().contains("$2a$hash"));
  }

  @Test
  @DisplayName("測試 register api")
  public void testRegister() throws Exception {
    when(userService.userRegister(any(MyUser.class)))
        .thenReturn(testSessionResponse);

    mockMvc.perform(post("/api/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Login successful"));
  }

  @Test
  @DisplayName("測試 addSuperAccount api")
  public void testAddSuperAccount() throws Exception {
    mockMvc.perform(get("/api/addSuperAccount").with(ADMIN))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("測試 getAllUsers api")
  public void testGetAllUsers() throws Exception {
    mockMvc.perform(get("/api/getAllUsers").with(ADMIN))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("測試 deleteUser api")
  public void testDeleteUserSuccess() throws Exception {
    when(userService.deleteUserById(1L)).thenReturn(true);
    mockMvc.perform(post("/api/deleteUser").with(ADMIN).param("userId", String.valueOf(1L)))
        .andExpect(status().isOk())
        .andExpect(content().string("true"));
  }

  @Test
  @DisplayName("測試 deleteUser api")
  public void testDeleteUserFailed() throws Exception {
    when(userService.deleteUserById(1L)).thenReturn(false);
    mockMvc.perform(post("/api/deleteUser").with(ADMIN).param("userId", String.valueOf(1L)))
        .andExpect(status().isOk())
        .andExpect(content().string("false"));
  }

  @Test
  @DisplayName("未登入呼叫需權限的 api 回傳 401")
  public void testUnauthenticated() throws Exception {
    mockMvc.perform(get("/api/getProjectNames").param("userId", "1"))
        .andExpect(status().isUnauthorized());
    mockMvc.perform(post("/api/deleteUser").param("userId", "1"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("一般使用者呼叫管理員 api 回傳 403")
  public void testUserCannotCallAdminApi() throws Exception {
    mockMvc.perform(get("/api/getAllUsers").with(USER))
        .andExpect(status().isForbidden());
    mockMvc.perform(post("/api/deleteUser").with(USER).param("userId", "1"))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/addSuperAccount").with(USER))
        .andExpect(status().isForbidden());
    verify(userService, never()).deleteUserById(any());
  }

  @Test
  @DisplayName("非專案擁有者無法存取專案資料")
  public void testNotProjectOwner() throws Exception {
    when(dataBaseService.isProjectOwner(1L, "otherProject")).thenReturn(false);
    when(dataBaseService.isMethodOwner(1L, 5L)).thenReturn(false);

    mockMvc.perform(get("/api/getProjectStructure").with(USER)
            .param("projectName", "otherProject"))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/pullProject").with(USER)
            .param("projectName", "otherProject"))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/getDiffInfoByMethod").with(USER)
            .param("methodId", "5"))
        .andExpect(status().isForbidden());
    verify(gitPuller, never()).pullLocalRepository(any(), anyInt());
  }

  @Test
  @DisplayName("DEMO 呼叫寫入或耗資源的 api 回傳 403，且沒有執行任何動作")
  public void testDemoCannotCallWriteApi() throws Exception {
    // 讓 controller 的擁有權檢查通過，確保 403 來自 SecurityConfig
    when(dataBaseService.isProjectOwner(2L, "x")).thenReturn(true);

    mockMvc.perform(post("/api/fetch-repo").with(DEMO)
            .param("url", "https://github.com/test/repo")
            .param("commitId", "main"))
        .andExpect(status().isForbidden());
    mockMvc.perform(post("/api/get-method-diff-temporary").with(DEMO)
            .param("url", "https://github.com/test/repo")
            .param("commitId", "main"))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/pullProject").with(DEMO).param("projectName", "x"))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/deleteData").with(DEMO).param("projectName", "x"))
        .andExpect(status().isForbidden());

    verify(gitCloner, never()).cloneRepository(any(), any(), any(), anyInt());
    verify(gitCloner, never()).temporaryCloneRepository(any(), any(), anyInt());
    verify(gitPuller, never()).pullLocalRepository(any(), anyInt());
    verify(dataBaseService, never()).deleteDataByProjectName(any(), any());
  }

  @Test
  @DisplayName("DEMO 的讀取 api 維持可用，且仍檢查擁有權")
  public void testDemoCanCallReadApi() throws Exception {
    when(dataBaseService.getUserProjects("2")).thenReturn(List.of("demoProject"));
    when(dataBaseService.isProjectOwner(2L, "x")).thenReturn(true);
    when(dataBaseService.getProjectStructure("x"))
        .thenReturn(ProjectStructureDTO.builder().projectName("x").build());
    when(dataBaseService.isMethodOwner(2L, 1L)).thenReturn(true);
    when(dataBaseService.getDiffInfoByMethodId(1L)).thenReturn(List.of());

    mockMvc.perform(get("/api/getProjectNames").with(DEMO))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/getProjectStructure").with(DEMO).param("projectName", "x"))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/getDiffInfoByMethod").with(DEMO).param("methodId", "1"))
        .andExpect(status().isOk());

    verify(dataBaseService).isProjectOwner(2L, "x");
    verify(dataBaseService).isMethodOwner(2L, 1L);
  }

  @Test
  @DisplayName("DEMO 呼叫管理員 api 回傳 403")
  public void testDemoCannotCallAdminApi() throws Exception {
    mockMvc.perform(get("/api/getAllUsers").with(DEMO))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("ADMIN 呼叫 fetch-repo 不是 403；未登入回傳 401")
  public void testAdminAllowedAndUnauthenticatedFetchRepo() throws Exception {
    mockMvc.perform(post("/api/fetch-repo").with(ADMIN)
            .param("url", "https://github.com/test/repo")
            .param("commitId", "main"))
        .andExpect(result -> assertFalse(result.getResponse().getStatus() == 403));
    mockMvc.perform(post("/api/fetch-repo")
            .param("url", "https://github.com/test/repo")
            .param("commitId", "main"))
        .andExpect(status().isUnauthorized());
  }

  private void exhaustAnalysisQuota() {
    when(quotaService.tryAcquire(QuotaService.Kind.ANALYSIS)).thenReturn(false);
  }

  @Test
  @DisplayName("分析額度用完：三個端點皆回 429，且不進入 clone／pull；三者共用 ANALYSIS 額度")
  public void testAnalysisQuotaExceeded() throws Exception {
    exhaustAnalysisQuota();
    when(dataBaseService.isProjectOwner(1L, "testProject")).thenReturn(true);

    mockMvc.perform(post("/api/fetch-repo").with(USER)
            .param("url", "https://github.com/test/repo").param("commitId", "main"))
        .andExpect(status().isTooManyRequests())
        .andExpect(content().string("今日分析額度已用完，請明日再試"));
    mockMvc.perform(post("/api/get-method-diff-temporary").with(USER)
            .param("url", "https://github.com/test/repo").param("commitId", "main"))
        .andExpect(status().isTooManyRequests());
    mockMvc.perform(get("/api/pullProject").with(USER).param("projectName", "testProject"))
        .andExpect(status().isTooManyRequests());

    verify(quotaService, times(3)).tryAcquire(QuotaService.Kind.ANALYSIS);
    verify(gitCloner, never()).cloneRepository(any(), any(), any(), anyInt());
    verify(gitCloner, never()).temporaryCloneRepository(any(), any(), anyInt());
    verify(gitPuller, never()).pullLocalRepository(any(), anyInt());
  }

  @Test
  @DisplayName("以嘗試次數計：clone 失敗（500）與 URL 不合法（400）也消耗額度")
  public void testFailedAttemptsConsumeQuota() throws Exception {
    when(gitCloner.cloneRepository(anyString(), anyString(), eq(1L), anyInt()))
        .thenThrow(new IOException("boom"));
    when(gitCloner.temporaryCloneRepository(anyString(), anyString(), anyInt()))
        .thenThrow(new IllegalArgumentException("bad url"));

    mockMvc.perform(post("/api/fetch-repo").with(USER)
            .param("url", "https://github.com/test/repo").param("commitId", "main"))
        .andExpect(status().isInternalServerError());
    mockMvc.perform(post("/api/get-method-diff-temporary").with(USER)
            .param("url", "bad").param("commitId", "main"))
        .andExpect(status().isBadRequest());

    verify(quotaService, times(2)).tryAcquire(QuotaService.Kind.ANALYSIS);
  }

  @Test
  @DisplayName("pullProject 非擁有者回 403 且不消耗額度")
  public void testPullProjectNotOwnerDoesNotConsumeQuota() throws Exception {
    when(dataBaseService.isProjectOwner(1L, "otherProject")).thenReturn(false);

    mockMvc.perform(get("/api/pullProject").with(USER).param("projectName", "otherProject"))
        .andExpect(status().isForbidden());

    verify(quotaService, never()).tryAcquire(any());
  }

  @Test
  @DisplayName("ADMIN 不受分析額度限制，也不消耗額度")
  public void testAdminBypassesAnalysisQuota() throws Exception {
    exhaustAnalysisQuota();
    when(gitCloner.cloneRepository(anyString(), anyString(), eq(99L), anyInt()))
        .thenReturn(GitResult.builder().status(GitStatus.CLONE_SUCCESS).message("ok").build());
    when(gitCloner.temporaryCloneRepository(anyString(), anyString(), anyInt()))
        .thenReturn(new GitDiffAnalyzer.TemporaryAnalysis(testProject, false, 500));
    when(dataBaseService.isProjectOwner(99L, "testProject")).thenReturn(true);
    when(gitPuller.pullLocalRepository(any(RepositoryINFO.class), anyInt()))
        .thenReturn(GitResult.builder().status(GitStatus.PULL_SUCCESS).message("ok").build());

    mockMvc.perform(post("/api/fetch-repo").with(ADMIN)
            .param("url", "https://github.com/test/repo").param("commitId", "main"))
        .andExpect(status().isOk());
    mockMvc.perform(post("/api/get-method-diff-temporary").with(ADMIN)
            .param("url", "https://github.com/test/repo").param("commitId", "main"))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/pullProject").with(ADMIN).param("projectName", "testProject"))
        .andExpect(status().isOk());

    verify(quotaService, never()).tryAcquire(any());
  }

  @Test
  @DisplayName("瀏覽既有專案不呼叫額度服務")
  public void testReadEndpointsDoNotTouchQuota() throws Exception {
    when(dataBaseService.getUserProjects("1")).thenReturn(List.of("p"));

    mockMvc.perform(get("/api/getProjectNames").with(USER)).andExpect(status().isOk());

    verify(quotaService, never()).tryAcquire(any());
  }

  private static final String REPO = "https://github.com/test/repo";

  private GitResult ok() {
    return GitResult.builder().status(GitStatus.CLONE_SUCCESS).message("ok").build();
  }

  private int capturedCloneLimit(long userId) throws Exception {
    ArgumentCaptor<Integer> captor = forClass(Integer.class);
    verify(gitCloner).cloneRepository(anyString(), anyString(), eq(userId), captor.capture());
    return captor.getValue();
  }

  @Test
  @DisplayName("fetch-repo：USER 用 USER 上限，ADMIN 用 ADMIN 上限")
  public void testFetchRepoCommitLimitByRole() throws Exception {
    when(gitCloner.cloneRepository(anyString(), anyString(), any(Long.class), anyInt()))
        .thenReturn(ok());

    mockMvc.perform(post("/api/fetch-repo").with(USER).param("url", REPO).param("commitId", "H"))
        .andExpect(status().isOk());
    assertEquals(USER_MAX, capturedCloneLimit(1L));

    mockMvc.perform(post("/api/fetch-repo").with(ADMIN).param("url", REPO).param("commitId", "H"))
        .andExpect(status().isOk());
    assertEquals(ADMIN_MAX, capturedCloneLimit(99L));
  }

  @Test
  @DisplayName("get-method-diff-temporary：依角色傳入 commit 上限")
  public void testTemporaryCommitLimitByRole() throws Exception {
    when(gitCloner.temporaryCloneRepository(anyString(), anyString(), anyInt()))
        .thenReturn(new GitDiffAnalyzer.TemporaryAnalysis(testProject, false, 0));

    mockMvc.perform(post("/api/get-method-diff-temporary").with(USER)
        .param("url", REPO).param("commitId", "H")).andExpect(status().isOk());
    verify(gitCloner).temporaryCloneRepository(REPO, "H", USER_MAX);
    mockMvc.perform(post("/api/get-method-diff-temporary").with(ADMIN)
        .param("url", REPO).param("commitId", "H")).andExpect(status().isOk());
    verify(gitCloner).temporaryCloneRepository(REPO, "H", ADMIN_MAX);
  }

  @Test
  @DisplayName("pullProject：依角色傳入 commit 上限")
  public void testPullCommitLimitByRole() throws Exception {
    when(dataBaseService.isProjectOwner(1L, "p")).thenReturn(true);
    when(dataBaseService.isProjectOwner(99L, "p")).thenReturn(true);
    when(gitPuller.pullLocalRepository(any(RepositoryINFO.class), anyInt()))
        .thenReturn(GitResult.builder().status(GitStatus.PULL_SUCCESS).message("ok").build());

    mockMvc.perform(get("/api/pullProject").with(USER).param("projectName", "p"))
        .andExpect(status().isOk());
    verify(gitPuller).pullLocalRepository(any(RepositoryINFO.class), eq(USER_MAX));
    mockMvc.perform(get("/api/pullProject").with(ADMIN).param("projectName", "p"))
        .andExpect(status().isOk());
    verify(gitPuller).pullLocalRepository(any(RepositoryINFO.class), eq(ADMIN_MAX));
  }

  @Test
  @DisplayName("缺少 role claim 的 JWT 視為 USER 上限")
  public void testNoRoleClaimUsesUserLimit() throws Exception {
    when(gitCloner.cloneRepository(anyString(), anyString(), any(Long.class), anyInt()))
        .thenReturn(ok());

    mockMvc.perform(post("/api/fetch-repo").with(NO_ROLE).param("url", REPO)
        .param("commitId", "H")).andExpect(status().isOk());
    assertEquals(USER_MAX, capturedCloneLimit(1L));
  }

  @Test
  @DisplayName("角色只來自 JWT，請求參數的 role 被忽略")
  public void testRoleParamIgnored() throws Exception {
    when(gitCloner.cloneRepository(anyString(), anyString(), any(Long.class), anyInt()))
        .thenReturn(ok());

    mockMvc.perform(post("/api/fetch-repo").with(USER).param("url", REPO)
        .param("commitId", "H").param("role", "ADMIN")).andExpect(status().isOk());
    assertEquals(USER_MAX, capturedCloneLimit(1L));
  }

  @Test
  @DisplayName("截斷註記：fetch-repo、pullProject 的 message 帶上限，temporary 帶 header")
  public void testTruncationNotes() throws Exception {
    String note = GitDiffAnalyzer.truncationNote(USER_MAX);
    when(gitCloner.cloneRepository(anyString(), anyString(), any(Long.class), anyInt()))
        .thenReturn(GitResult.builder().status(GitStatus.CLONE_SUCCESS)
            .message("成功將資料分類完成" + note).build());
    when(dataBaseService.isProjectOwner(1L, "p")).thenReturn(true);
    when(gitPuller.pullLocalRepository(any(RepositoryINFO.class), anyInt()))
        .thenReturn(GitResult.builder().status(GitStatus.PULL_SUCCESS)
            .message("成功 Pull 並更新資料" + note).build());
    when(gitCloner.temporaryCloneRepository(anyString(), anyString(), anyInt()))
        .thenReturn(new GitDiffAnalyzer.TemporaryAnalysis(testProject, true, USER_MAX));

    mockMvc.perform(post("/api/fetch-repo").with(USER).param("url", REPO).param("commitId", "H"))
        .andExpect(jsonPath("$.message").value(Matchers.containsString("僅分析最新 3")));
    mockMvc.perform(get("/api/pullProject").with(USER).param("projectName", "p"))
        .andExpect(jsonPath("$.message").value(Matchers.containsString("僅分析最新 3")));
    mockMvc.perform(post("/api/get-method-diff-temporary").with(USER)
            .param("url", REPO).param("commitId", "H"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Analysis-Truncated", "3"));
  }

  @Test
  @DisplayName("未截斷時 temporary 沒有 X-Analysis-Truncated header")
  public void testNoTruncationHeader() throws Exception {
    when(gitCloner.temporaryCloneRepository(anyString(), anyString(), anyInt()))
        .thenReturn(new GitDiffAnalyzer.TemporaryAnalysis(testProject, false, USER_MAX));

    mockMvc.perform(post("/api/get-method-diff-temporary").with(USER)
            .param("url", REPO).param("commitId", "H"))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("X-Analysis-Truncated"));
  }

  @Test
  @DisplayName("槽位被佔用：三個入口立即回 429，不呼叫 clone／pull，也不扣每日額度")
  public void testBusySlotRejectsAll() throws Exception {
    assertTrue(analysisLimits.tryAcquire());
    when(dataBaseService.isProjectOwner(1L, "p")).thenReturn(true);

    mockMvc.perform(post("/api/fetch-repo").with(USER).param("url", REPO).param("commitId", "H"))
        .andExpect(status().isTooManyRequests())
        .andExpect(content().string("目前已有其他分析正在執行，請稍後再試"));
    mockMvc.perform(post("/api/get-method-diff-temporary").with(USER)
        .param("url", REPO).param("commitId", "H")).andExpect(status().isTooManyRequests());
    mockMvc.perform(get("/api/pullProject").with(USER).param("projectName", "p"))
        .andExpect(status().isTooManyRequests());

    verify(quotaService, never()).tryAcquire(any());
    verify(gitCloner, never()).cloneRepository(any(), any(), any(), anyInt());
    verify(gitCloner, never()).temporaryCloneRepository(any(), any(), anyInt());
    verify(gitPuller, never()).pullLocalRepository(any(), anyInt());
  }

  @Test
  @DisplayName("槽位在例外路徑也會釋放：IOException、IllegalArgumentException、RuntimeException")
  public void testSlotReleasedOnException() throws Exception {
    Throwable[] errors = {new IOException("io"), new IllegalArgumentException("arg"),
        new RuntimeException("rt")};
    for (Throwable error : errors) {
      doThrow(error).when(gitCloner)
          .cloneRepository(anyString(), anyString(), any(Long.class), anyInt());
      try {
        mockMvc.perform(post("/api/fetch-repo").with(USER).param("url", REPO)
            .param("commitId", "H"));
      } catch (Exception expected) {
        // RuntimeException 會被 MockMvc 包成 ServletException，這裡只關心槽位是否釋放
      }
    }
    doReturn(ok()).when(gitCloner)
        .cloneRepository(anyString(), anyString(), any(Long.class), anyInt());

    mockMvc.perform(post("/api/fetch-repo").with(USER).param("url", REPO).param("commitId", "H"))
        .andExpect(status().isOk());
    assertTrue(analysisLimits.tryAcquire());
  }

  @Test
  @DisplayName("專案數達上限：USER 回 403，不呼叫 clone，也不扣每日額度；未達上限則放行")
  public void testProjectLimit() throws Exception {
    when(dataBaseService.getUserProjects("1")).thenReturn(List.of("a", "b"));

    mockMvc.perform(post("/api/fetch-repo").with(USER).param("url", REPO).param("commitId", "H"))
        .andExpect(status().isForbidden())
        .andExpect(content().string(Matchers.containsString("專案數上限")));
    verify(gitCloner, never()).cloneRepository(any(), any(), any(), anyInt());
    verify(quotaService, never()).tryAcquire(any());
    // 被拒絕後槽位已釋放
    assertTrue(analysisLimits.tryAcquire());
    analysisLimits.release();

    when(dataBaseService.getUserProjects("1")).thenReturn(List.of("a"));
    when(gitCloner.cloneRepository(anyString(), anyString(), any(Long.class), anyInt()))
        .thenReturn(ok());
    mockMvc.perform(post("/api/fetch-repo").with(USER).param("url", REPO).param("commitId", "H"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("ADMIN 不受專案數限制，也不查專案數")
  public void testAdminNoProjectLimit() throws Exception {
    when(dataBaseService.getUserProjects("99")).thenReturn(List.of("a", "b", "c", "d"));
    when(gitCloner.cloneRepository(anyString(), anyString(), any(Long.class), anyInt()))
        .thenReturn(ok());

    mockMvc.perform(post("/api/fetch-repo").with(ADMIN).param("url", REPO).param("commitId", "H"))
        .andExpect(status().isOk());
    verify(dataBaseService, never()).getUserProjects("99");
  }

  @Test
  @DisplayName("磁碟空間不足：fetch-repo 與 get-method-diff-temporary 回 503")
  public void testInsufficientDisk() throws Exception {
    when(gitCloner.cloneRepository(anyString(), anyString(), any(Long.class), anyInt()))
        .thenThrow(new AnalysisLimits.InsufficientDiskSpaceException("伺服器磁碟空間不足"));
    when(gitCloner.temporaryCloneRepository(anyString(), anyString(), anyInt()))
        .thenThrow(new AnalysisLimits.InsufficientDiskSpaceException("伺服器磁碟空間不足"));

    mockMvc.perform(post("/api/fetch-repo").with(USER).param("url", REPO).param("commitId", "H"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().string("伺服器磁碟空間不足"));
    mockMvc.perform(post("/api/get-method-diff-temporary").with(USER)
            .param("url", REPO).param("commitId", "H"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().string("伺服器磁碟空間不足"));
    assertTrue(analysisLimits.tryAcquire());
  }
}
