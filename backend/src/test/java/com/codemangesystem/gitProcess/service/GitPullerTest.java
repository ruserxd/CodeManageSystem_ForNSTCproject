package com.codemangesystem.gitProcess.service;

import com.codemangesystem.gitProcess.model_Git.GitResult;
import com.codemangesystem.gitProcess.model_Git.GitStatus;
import com.codemangesystem.gitProcess.model_Repo.RepositoryINFO;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GitPullerTest {
    RepositoryINFO repositoryINFO;
    DataBaseService dataBaseService;
    GitDiffAnalyzer gitDiffAnalyzer;
    GitPuller gitPuller;


    @BeforeEach
    void setUp() {
        repositoryINFO = RepositoryINFO.builder()
                                       .repoName("test")
                                       .localPath("/test")
                                       .build();
        dataBaseService = mock(DataBaseService.class);
        gitDiffAnalyzer = mock(GitDiffAnalyzer.class);
        gitPuller = new GitPuller(dataBaseService, gitDiffAnalyzer);
        gitPuller = spy(gitPuller);
    }

    @Nested
    class pullLocalRepository {
        @Test
        @DisplayName("測試 pull 失敗")
        void pullFailedTest() throws GitAPIException {
            Git git = mock(Git.class);
            try (MockedStatic<Git> gitMock = mockStatic(Git.class)){
                gitMock.when(()-> Git.open(any(File.class))).thenReturn(git);

                PullCommand pullCommand = mock(PullCommand.class);
                when(git.pull()).thenReturn(pullCommand);
                when(pullCommand.setRemote(anyString())).thenReturn(pullCommand);
                when(pullCommand.setRemoteBranchName(anyString())).thenReturn(pullCommand);

                PullResult pullResult = mock(PullResult.class);
                when(pullCommand.call()).thenReturn(pullResult);

                when(pullResult.isSuccessful()).thenReturn(false);

                //執行測試
                GitResult excepted = GitResult.builder()
                                              .message("發生不可預期 Failed pull test")
                                              .status(GitStatus.PULL_FAILED)
                                              .build();
                GitResult actual = gitPuller.pullLocalRepository(repositoryINFO);
                assertEquals(excepted, actual);

                verify(pullResult, times(1)).isSuccessful();
            }
            git.close();
        }

        @Test
        @DisplayName("測試 pull 成功")
        void pullSuccessTest() throws GitAPIException, IOException {
            Git git = mock(Git.class);
            try (MockedStatic<Git> gitMock = mockStatic(Git.class)){
                gitMock.when(()-> Git.open(any(File.class))).thenReturn(git);

                PullCommand pullCommand = mock(PullCommand.class);
                when(git.pull()).thenReturn(pullCommand);
                when(pullCommand.setRemote(anyString())).thenReturn(pullCommand);
                when(pullCommand.setRemoteBranchName(anyString())).thenReturn(pullCommand);

                PullResult pullResult = mock(PullResult.class);
                when(pullCommand.call()).thenReturn(pullResult);

                when(pullResult.isSuccessful()).thenReturn(true);

                when(dataBaseService.getHeadRevstr(anyString())).thenReturn("123456789");

                // 分析完預計回傳
                GitResult excepted = GitResult.builder()
                                              .status(GitStatus.PULL_SUCCESS)
                                              .message("成功 Pull 並更新資料")
                                              .build();
                when(gitDiffAnalyzer.analyzePartCommits(anyString(), anyString())).thenReturn(excepted);

                //執行測試
                GitResult actual = gitPuller.pullLocalRepository(repositoryINFO);
                assertEquals(excepted, actual);

                verify(pullResult, times(1)).isSuccessful();
            }
            git.close();
        }

        @Test
        @DisplayName("測試 IOException")
        void throwIOExceptionTest() {
            Git git = mock(Git.class);
            try (MockedStatic<Git> gitMock = mockStatic(Git.class)){
                gitMock.when(()-> Git.open(any(File.class))).thenThrow(IOException.class);

                // 分析完預計回傳
                GitResult excepted = GitResult.builder()
                                              .status(GitStatus.PULL_FAILED)
                                              .message("Pull 發生 null")
                                              .build();

                //執行測試
                GitResult actual = gitPuller.pullLocalRepository(repositoryINFO);
                assertEquals(excepted, actual);
            }
            git.close();
        }

        @Test
        @DisplayName("測試 GitAPIException")
        void throwGitAPIExceptionTest() throws GitAPIException{
            Git git = mock(Git.class);
            try (MockedStatic<Git> gitMock = mockStatic(Git.class)){
                gitMock.when(()-> Git.open(any(File.class))).thenReturn(git);

                PullCommand pullCommand = mock(PullCommand.class);
                when(git.pull()).thenReturn(pullCommand);
                when(pullCommand.setRemote(anyString())).thenReturn(pullCommand);
                when(pullCommand.setRemoteBranchName(anyString())).thenReturn(pullCommand);

                PullResult pullResult = mock(PullResult.class);
                when(pullCommand.call()).thenThrow(new GitAPIException("pull failed"){});

                // 分析完預計回傳
                GitResult excepted = GitResult.builder()
                                              .status(GitStatus.PULL_FAILED)
                                              .message("Pull 發生 pull failed")
                                              .build();

                //執行測試
                GitResult actual = gitPuller.pullLocalRepository(repositoryINFO);
                assertEquals(excepted, actual);
            }
            git.close();
        }
    }
}