package com.codemangesystem.gitProcess.service;

import com.codemangesystem.gitProcess.model_Repo.RepositoryINFO;
import com.codemangesystem.gitProcess.repository.PersonalRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
class GitFunctionTest {
    @Nested
    class getRepoNameFromUrlTest {
        @Test
        @DisplayName("測試正常功能的情況")
        void urlResultTest() {
            String repoUrl = "https://github.com/ruserxd/CodeMangeSystem_ForNSTCproject.git";
            String actual = GitFunction.getRepoNameFromUrl(repoUrl);
            assertEquals("CodeMangeSystem_ForNSTCproject", actual);
        }

        @Test
        @DisplayName("測試正常功能的情況 2")
        void urlResultTest2() {
            String repoUrl = "https://github.com/ruserxd/ARPG_GAME.git";
            String actual = GitFunction.getRepoNameFromUrl(repoUrl);
            assertEquals("ARPG_GAME", actual);
        }
    }

    @Nested
    class isLocalClonedTest {
        @Test
        void gitFileExist() {
        }

        @Test
        void gitFileNotExist() {
            boolean actual = GitFunction.isLocalCloned("/test");
            assertFalse(actual);
        }
    }

    @Nested
    class isUserClonedTest {
        Long userId;
        RepositoryINFO repositoryINFO;
        PersonalRepository personalRepository;

        @BeforeEach
        void setUp() {
            userId = 1L;
            repositoryINFO = RepositoryINFO.builder()
                                           .localPath("/test")
                                           .repoName("test")
                                           .build();
            personalRepository = mock(PersonalRepository.class);
        }

        @Test
        @DisplayName("測試使用者有 clone 過該專案")
        void userClonedTest() {
            List<String> userProjects = new ArrayList<>();
            userProjects.add("test2");
            userProjects.add("test3");
            userProjects.add("test");

            when(personalRepository.findProjectNameByUserId(userId))
                    .thenReturn(userProjects);

            boolean actual = GitFunction.isUserCloned(userId, repositoryINFO, personalRepository);
            assertTrue(actual);
        }

        @Test
        @DisplayName("測試使用者沒有 clone 過該專案")
        void userNotClonedTest() {
            List<String> userProjects = new ArrayList<>();
            userProjects.add("test2");
            userProjects.add("test3");
            userProjects.add("test4");

            when(personalRepository.findProjectNameByUserId(userId))
                    .thenReturn(userProjects);

            boolean actual = GitFunction.isUserCloned(userId, repositoryINFO, personalRepository);
            assertFalse(actual);
        }
    }
}