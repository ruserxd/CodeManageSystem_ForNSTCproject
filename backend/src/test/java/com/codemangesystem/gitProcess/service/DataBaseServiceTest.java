package com.codemangesystem.gitProcess.service;

import com.codemangesystem.gitProcess.model_DataBase.Project;
import com.codemangesystem.gitProcess.repository.PersonalRepository;
import com.codemangesystem.gitProcess.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

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
        List<String>except = dataBaseService.getUserProjects("1");

        // 確認
        log.info("getUserProjectsTest():");
        for (int i = 0; i < TIME; i++) {
            log.info("Except: {}, Actual: {}", "Name" + i, except.get(i));
            assertEquals("Name" + i, except.get(i));
        }
    }

    @Test
    @DisplayName("測試 getProjectByProjectName")
    void getProjectByProjectNameTest() {
        String projectName = "test";
        Project except = Project.builder()
                                .projectName(projectName)
                                .build();
        Mockito.when(projectRepository.findByProjectName("test")).thenReturn(except);

        Project actual = dataBaseService.getProjectByProjectName(projectName);
        log.info("getProjectByProjectNameTest(): Except: {}, Actual: {}", except, actual);
        assertEquals(except, actual);
    }

    @Test
    @DisplayName("測試 deleteDataByProjectName() 沒有找到對應的 PersonalInfo")
    void deleteDataByProjectName_no_personalINFO_Test() {
        String projectName = "test";
        String userId = "1";


        Mockito.when(personalRepository.findProjectByUserIdAndProjectName(projectName, Long.parseLong(userId)))
               .thenReturn(Optional.empty());

        String actual = dataBaseService.deleteDataByProjectName(projectName, userId);
        assertEquals("No personalINFO found to delete", actual);

    }

    @Test
    @DisplayName("測試 deleteDataByProjectName()")
    void deleteDataByProjectName_Test() {
        String projectName = "test";
        String userId = "1";


        Mockito.when(personalRepository.findProjectByUserIdAndProjectName(projectName, Long.parseLong(userId)))
               .thenReturn(Optional.empty());

        String actual = dataBaseService.deleteDataByProjectName(projectName, userId);
        assertEquals("No personalINFO found to delete", actual);

    }
}