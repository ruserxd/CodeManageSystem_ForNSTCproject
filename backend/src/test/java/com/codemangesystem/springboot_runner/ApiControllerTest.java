package com.codemangesystem.springboot_runner;

import com.codemangesystem.ApiController;
import com.codemangesystem.git_process.model_database.Project;
import com.codemangesystem.git_process.model_git.GitResult;
import com.codemangesystem.git_process.model_git.GitStatus;
import com.codemangesystem.git_process.model_repo.RepositoryINFO;
import com.codemangesystem.git_process.service.DataBaseService;
import com.codemangesystem.git_process.service.GitCloner;
import com.codemangesystem.git_process.service.GitPuller;
import com.codemangesystem.login_process.model_response.LoginINFO;
import com.codemangesystem.login_process.model_response.SessionResponse;
import com.codemangesystem.login_process.model_user.MyUser;
import com.codemangesystem.login_process.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GitCloner gitCloner;

    @MockBean
    private DataBaseService dataBaseService;

    @MockBean
    private UserService userService;

    @MockBean
    private GitPuller gitPuller;

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
        when(gitCloner.cloneRepository(anyString(), anyString(), any(Long.class)))
                .thenReturn(mockResult);

        // 測試與驗證
        mockMvc.perform(post("/api/fetch-repo")
                       .param("url", "https://github.com/test/repo")
                       .param("commitId", "main")
                       .param("userId", "1"))
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
                       .param("url", "https://github.com/test/repo")
                       .param("commitId", "main")
                       .param("userId", "1"))
               .andExpect(status().isInternalServerError())
               .andExpect(content().string("clone 或存取儲存庫時發生錯誤。請檢查 URL 是否正確。"));
    }

    @Test
    @DisplayName("測試 getProjectNames api")
    public void testGetProjectNames() throws Exception {
        List<String> projectNames = Arrays.asList("project1", "project2");
        when(dataBaseService.getUserProjects(anyString())).thenReturn(projectNames);

        mockMvc.perform(get("/api/getProjectNames")
                       .param("userId", "1"))
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

        mockMvc.perform(get("/api/pullProject")
                       .param("projectName", "testProject"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("PULL_SUCCESS"));
    }

    @Test
    @DisplayName("測試 getFileDataByProjectName api")
    public void testGetFileDataByProjectName() throws Exception {
        when(dataBaseService.getProjectByProjectName(anyString()))
                .thenReturn(testProject);

        mockMvc.perform(post("/api/getData")
                       .param("ProjectName", "testProject"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.projectName").value("testProject"));
    }

    @Test
    @DisplayName("測試 deleteDataByProjectName api")
    public void testDeleteDataByProjectName() throws Exception {
        when(dataBaseService.deleteDataByProjectName(anyString(), anyString()))
                .thenReturn("Deleted successfully");

        mockMvc.perform(get("/api/deleteData")
                       .param("projectName", "testProject")
                       .param("userId", "1"))
               .andExpect(status().isOk())
               .andExpect(content().string("Deleted successfully"));
    }

    @Test
    @DisplayName("測試 login api")
    public void testLogin() throws Exception {
        when(userService.checkUser(any(LoginINFO.class)))
                .thenReturn(testSessionResponse);

        mockMvc.perform(post("/api/login")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(testLoginInfo)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.message").value("Login successful"));
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
        mockMvc.perform(get("/api/addSuperAccount"));
    }
}