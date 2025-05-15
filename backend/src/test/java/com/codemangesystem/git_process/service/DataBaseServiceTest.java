package com.codemangesystem.git_process.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.codemangesystem.git_process.model.database.PersonalINFO;
import com.codemangesystem.git_process.model.database.Project;
import com.codemangesystem.git_process.repository.PersonalRepository;
import com.codemangesystem.git_process.repository.ProjectRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Slf4j
class DataBaseServiceTest {

  private ProjectRepository projectRepository;
  private PersonalRepository personalRepository;
  private DataBaseService dataBaseService;

  @BeforeEach
  void setUp() {
    personalRepository = Mockito.mock(PersonalRepository.class);
    projectRepository = Mockito.mock(ProjectRepository.class);
    dataBaseService = new DataBaseService(projectRepository, personalRepository);
  }

  @Nested
  class getUserProjects {

    @Test
    @DisplayName("測試 getUserProjects")
    void getUserProjectsTest() {
      final int TIME = 5;
      // 設定 Repository 回傳資料
      List<Long> exceptProjectId = new ArrayList<>();
      for (Long i = 0L; i < TIME; i++) {
        exceptProjectId.add(i);
        Mockito.when(projectRepository.findProjectNameByProjectId(i)).thenReturn("Name" + i);
      }
      Mockito.when(personalRepository.findProjectIdByUserId(1L)).thenReturn(exceptProjectId);

      // 呼叫 DataBaseService
      List<String> except = dataBaseService.getUserProjects("1");

      // 確認
      log.info("getUserProjectsTest():");
      for (int i = 0; i < TIME; i++) {
        log.info("Except: {}, Actual: {}", "Name" + i, except.get(i));
        assertEquals("Name" + i, except.get(i));
      }
    }
  }

  @Nested
  class getProjectByProjectName {

    @Test
    @DisplayName("測試 getProjectByProjectName 有該 projectName")
    void getProjectByProjectName_have_projectName_Test() {
      // 設定要傳入的資料
      String projectName = "test";

      // 模擬預期結果
      Project except = Project.builder()
          .projectName(projectName)
          .build();
      Mockito.when(projectRepository.findByProjectName("test"))
          .thenReturn(Optional.ofNullable(except));

      // 確定
      Project actual = dataBaseService.getProjectByProjectName(projectName);
      log.info("getProjectByProjectNameTest(): Except: {}, Actual: {}", except, actual);
      assertEquals(except, actual);
    }

    @Test
    @DisplayName("測試 getProjectByProjectName 沒該 projectName")
    void getProjectByProjectName_no_projectName_Test() {
      // 設定要傳入的資料
      String projectName = "test";

      // 模擬預期結果
      Project except = null;
      Mockito.when(projectRepository.findByProjectName("test"))
          .thenReturn(Optional.empty());

      // 確定
      Project actual = dataBaseService.getProjectByProjectName(projectName);
      log.info("no projectName Except: {}, Actual: {}", except, actual);
      assertNull(actual);
    }
  }

  @Nested
  class deleteDataByProjectName {

    @Test
    @DisplayName("測試 deleteDataByProjectName() 沒有找到對應的 PersonalInfo")
    void deleteDataByProjectName_no_personalINFO_Test() {
      // 設定要傳入的資料
      String projectName = "test";
      String userId = "1";

      // 模擬預期結果
      Mockito.when(
              personalRepository.findProjectByUserIdAndProjectName(projectName, Long.parseLong(userId)))
          .thenReturn(Optional.empty());

      // 確定
      String actual = dataBaseService.deleteDataByProjectName(projectName, userId);
      PersonalINFO personalINFO = null;
      assertEquals("No personalINFO found to delete", actual);

      // 確定沒有執行到後面的程式碼
      Mockito.verify(personalRepository, Mockito.times(0)).delete(personalINFO);
      Mockito.verify(personalRepository, Mockito.times(0)).findProjectIdByProjectName(projectName);
    }

    @Test
    @DisplayName("測試 deleteDataByProjectName() 有該 PersonalINFO, userIds 是 empty")
    void deleteDataByProjectName_PersonalINFO_userIdsEmpty_Test() {
      // 設定要傳入的資料
      String projectName = "test";
      String userId = "1";

      // 模擬預期結果
      Project project = Project.builder()
          .projectName("test")
          .build();
      PersonalINFO personalINFO = PersonalINFO.builder()
          .project(project)
          .build();
      Mockito.when(
              personalRepository.findProjectByUserIdAndProjectName(projectName, Long.parseLong(userId)))
          .thenReturn(Optional.ofNullable(personalINFO));

      List<Long> list = new ArrayList<>();
      Mockito.when(personalRepository.findProjectIdByProjectName(projectName))
          .thenReturn(list);

      // 忽略 delete 操作
      Mockito.doNothing().when(projectRepository).delete(project);

      // 確定
      String actual = dataBaseService.deleteDataByProjectName(projectName, userId);
      assertEquals("Success delete", actual);

      // 確定是否有執行到後面的程式碼
      Mockito.verify(personalRepository, Mockito.times(1)).delete(personalINFO);
      Mockito.verify(personalRepository, Mockito.times(1)).findProjectIdByProjectName(projectName);
      Mockito.verify(projectRepository, Mockito.times(1)).delete(project);
    }

    @Test
    @DisplayName("測試 deleteDataByProjectName() 有該 PersonalINFO, userIds 也還有存在的")
    void deleteDataByProjectName_PersonalINFO_userIdsHave_Test() {
      // 設定要傳入的資料
      String projectName = "test";
      String userId = "1";

      // 模擬預期結果
      Project project = Project.builder()
          .projectId(1L)
          .projectName("test")
          .headRevstr("XXXXXXXX")
          .build();
      PersonalINFO personalINFO = PersonalINFO.builder()
          .project(project)
          .build();

      Mockito.when(
              personalRepository.findProjectByUserIdAndProjectName(projectName, Long.parseLong(userId)))
          .thenReturn(Optional.ofNullable(personalINFO));

      List<Long> list = new ArrayList<>();
      list.add(2L);
      Mockito.when(personalRepository.findProjectIdByProjectName(projectName))
          .thenReturn(list);

      // 確定
      String actual = dataBaseService.deleteDataByProjectName(projectName, userId);
      assertEquals("Success delete", actual);

      // 確定是否有執行到後面的程式碼
      Mockito.verify(personalRepository, Mockito.times(1)).delete(personalINFO);
      Mockito.verify(personalRepository, Mockito.times(1)).findProjectIdByProjectName(projectName);
      Mockito.verify(projectRepository, Mockito.times(0)).delete(project);
    }

    @Test
    @DisplayName("測試 deleteDataByProjectName() 刪除資料夾時拋出 IOException")
    void deleteDataByProjectName_PersonalINFO_IOException_Test() throws IOException {
      // 設定要傳入的資料
      String projectName = "test";
      String userId = "1";

      // 將 dataBaseService 轉換為 spy ，因為這邊需要模擬相同 class 的拋出情況
      DataBaseService spyService = Mockito.spy(dataBaseService);

      // 模擬預期結果
      Project project = Project.builder()
          .projectName("test")
          .build();
      PersonalINFO personalINFO = PersonalINFO.builder()
          .project(project)
          .build();
      Mockito.when(
              personalRepository.findProjectByUserIdAndProjectName(projectName, Long.parseLong(userId)))
          .thenReturn(Optional.ofNullable(personalINFO));

      List<Long> list = new ArrayList<>();
      Mockito.when(personalRepository.findProjectIdByProjectName(projectName))
          .thenReturn(list);

      // deleteGitRepository 拋出例外
      Mockito.doThrow(IOException.class).when(spyService)
          .deleteGitRepository(Mockito.anyString());

      // 確定
      String actual = spyService.deleteDataByProjectName(projectName, userId);
      assertEquals("Failed delete", actual);

      // 確定是否有執行到後面的程式碼
      Mockito.verify(personalRepository, Mockito.times(1)).delete(personalINFO);
      Mockito.verify(personalRepository, Mockito.times(1)).findProjectIdByProjectName(projectName);
      Mockito.verify(projectRepository, Mockito.times(1)).delete(project);
    }
  }

  @Nested
  class getHeadRevstr {

    @Test
    @DisplayName("測試 getHeadRevstr()")
    void getHeadRevstrTest() {
      // 傳入資料
      String projectName = "test";

      // 模擬預期結果
      Mockito.when(projectRepository.findHeadRevstrByProjectName(projectName))
          .thenReturn("testHead");

      // 確定
      String actual = dataBaseService.getHeadRevstr("test");
      assertEquals("testHead", actual);
    }
  }
}