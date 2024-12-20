package com.codemangesystem.gitProcess.service;

import com.codemangesystem.gitProcess.model_Git.GitResult;
import com.codemangesystem.gitProcess.model_Git.GitStatus;
import com.codemangesystem.gitProcess.model_Repo.RepositoryINFO;
import com.codemangesystem.gitProcess.repository.PersonalRepository;
import com.codemangesystem.gitProcess.repository.ProjectRepository;
import com.codemangesystem.loginProcess.model_user.MyUser;
import com.codemangesystem.loginProcess.repository.MyUserRepository;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GitClonerTest {
    // clone 存放的檔案位置
    private static final String CLONE_LOCAL_BASE_PATH = "src/cloneCode/";

    private GitDiffAnalyzer gitDiffAnalyzer;
    private MyUserRepository myUserRepository;
    private GitPuller gitPuller;
    private PersonalRepository personalRepository;
    private ProjectRepository projectRepository;
    private GitCloner gitCloner;

    @BeforeEach
    void setUp() {
        this.gitDiffAnalyzer = Mockito.mock(GitDiffAnalyzer.class);
        this.myUserRepository = Mockito.mock(MyUserRepository.class);
        this.gitPuller = Mockito.mock(GitPuller.class);
        this.personalRepository = Mockito.mock(PersonalRepository.class);
        this.projectRepository = Mockito.mock(ProjectRepository.class);

        this.gitCloner = new GitCloner(gitDiffAnalyzer, myUserRepository, gitPuller, personalRepository, projectRepository);
    }
    @Nested
    class cloneRepositoryTest {
        String repoUrl = "https://github.com/ruserxd/test.git";
        String commitId = "587a0e12610554a97b3aea6d6126ed92fb010865";
        Long userId = 1L;

        @BeforeEach
        void setUp() {
            Mockito.when(myUserRepository.findByUserId(userId))
                   .thenReturn(Optional.ofNullable(MyUser.builder()
                                                         .userId(1L)
                                                         .build()));
        }

        @Test
        @DisplayName("測試拋出 user 已經有 clone 過的例外")
        void isUserClonedTest() throws GitAPIException, IOException {
            try (MockedStatic<GitFunction> mockedStatic = Mockito.mockStatic(GitFunction.class)) {
                mockedStatic.when(() -> GitFunction.isUserCloned(Mockito.anyLong(),               // userId
                                                           Mockito.any(RepositoryINFO.class),  // repoINFO
                                                           Mockito.any(PersonalRepository.class)))
                            .thenReturn(true);
                GitResult result = gitCloner.cloneRepository(repoUrl, commitId, userId);
                GitResult excepted = GitResult.builder()
                                              .message("此帳戶已經有 clone 過 " + repoUrl)
                                              .status(GitStatus.CLONE_FAILED)
                                              .build();
                assertEquals(excepted,result);
            }
        }

        @Test
        @DisplayName("測試")
        void isCommitIdWrong () throws GitAPIException, IOException{
            try (MockedStatic<GitFunction> mockedStatic = Mockito.mockStatic(GitFunction.class)) {
                mockedStatic.when(() -> GitFunction.isUserCloned(Mockito.anyLong(),               // userId
                                    Mockito.any(RepositoryINFO.class),  // repoINFO
                                    Mockito.any(PersonalRepository.class)))
                            .thenReturn(false);
                mockedStatic.when(() -> GitFunction.isLocalCloned(Mockito.anyString()))
                            .thenReturn(true);
                // 跑到 pullAndUpdateDataBase
                GitResult result = gitCloner.cloneRepository(repoUrl, commitId, userId);
                // TODO: 未完成

                GitResult excepted = null;
                assertEquals(excepted,result);
            }
        }
    }

    @Nested
    class pullAndUpdateDataBaseTest {
    }

    @Nested
    class checkToCommitIdTest {

    }

}