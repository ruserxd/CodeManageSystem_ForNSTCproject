package com.codemangesystem.springboot_runner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import com.codemangesystem.git_process.service.GitPuller;
import com.codemangesystem.login_process.model.response.LoginINFO;
import com.codemangesystem.login_process.model.response.SessionResponse;
import com.codemangesystem.login_process.model.user.MyUser;
import com.codemangesystem.login_process.service.JwtService;
import com.codemangesystem.login_process.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ApiController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.jwt.secret=test-secret-test-secret-test-secret-123")
public class ApiControllerTest {

  // userId = 1 的一般使用者
  private static final RequestPostProcessor USER = jwt()
      .jwt(token -> token.subject("1"))
      .authorities(new SimpleGrantedAuthority("ROLE_USER"));

  private static final RequestPostProcessor ADMIN = jwt()
      .jwt(token -> token.subject("99"))
      .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));

  // userId = 2 的唯讀 demo 帳號
  private static final RequestPostProcessor DEMO = jwt()
      .jwt(token -> token.subject("2"))
      .authorities(new SimpleGrantedAuthority("ROLE_DEMO"));

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

  @Autowired
  private ObjectMapper objectMapper;

  private LoginINFO testLoginInfo;
  private MyUser testUser;
  private SessionResponse testSessionResponse;
  private Project testProject;

  @BeforeEach
  void setUp() {
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
    when(gitCloner.cloneRepository(anyString(), anyString(), eq(1L)))
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
    when(gitCloner.cloneRepository(anyString(), anyString(), any(Long.class)))
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
    when(gitCloner.cloneRepository(anyString(), anyString(), any(Long.class)))
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
    when(gitCloner.temporaryCloneRepository(anyString(), anyString()))
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

    when(gitPuller.pullLocalRepository(any(RepositoryINFO.class)))
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
    verify(gitPuller, never()).pullLocalRepository(any());
  }

  @Test
  @DisplayName("DEMO 呼叫寫入或耗資源的 api 回傳 403，且沒有執行任何動作")
  public void testDemoCannotCallWriteApi() throws Exception {
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

    verify(gitCloner, never()).cloneRepository(any(), any(), any());
    verify(gitCloner, never()).temporaryCloneRepository(any(), any());
    verify(gitPuller, never()).pullLocalRepository(any());
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
}
