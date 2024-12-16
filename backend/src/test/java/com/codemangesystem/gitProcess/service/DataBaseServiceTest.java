package com.codemangesystem.gitProcess.service;

import com.codemangesystem.gitProcess.model_DataBase.PersonalINFO;
import com.codemangesystem.gitProcess.model_DataBase.Project;
import com.codemangesystem.gitProcess.repository.PersonalRepository;
import com.codemangesystem.gitProcess.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

//TODO: 拋出例外的測試
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
            Mockito.when(personalRepository.findProjectByUserIdAndProjectName(projectName, Long.parseLong(userId)))
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
            Mockito.when(personalRepository.findProjectByUserIdAndProjectName(projectName, Long.parseLong(userId)))
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
                                     .projectName("test")
                                     .build();
            PersonalINFO personalINFO = PersonalINFO.builder()
                                                    .project(project)
                                                    .build();
            Mockito.when(personalRepository.findProjectByUserIdAndProjectName(projectName, Long.parseLong(userId)))
                   .thenReturn(Optional.ofNullable(personalINFO));
            List<Long> list = new ArrayList<>();

            // 有加入 id
            list.add(1L);
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