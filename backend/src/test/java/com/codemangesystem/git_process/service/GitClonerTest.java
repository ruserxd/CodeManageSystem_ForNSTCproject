package com.codemangesystem.git_process.service;

import com.codemangesystem.git_process.model_database.Project;
import com.codemangesystem.git_process.model_git.GitResult;
import com.codemangesystem.git_process.model_git.GitStatus;
import com.codemangesystem.git_process.model_repo.RepositoryINFO;
import com.codemangesystem.git_process.repository.PersonalRepository;
import com.codemangesystem.git_process.repository.ProjectRepository;
import com.codemangesystem.login_process.model_user.MyUser;
import com.codemangesystem.login_process.repository.MyUserRepository;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GitClonerTest {
    private GitDiffAnalyzer gitDiffAnalyzer;
    private MyUserRepository myUserRepository;
    private GitPuller gitPuller;
    private PersonalRepository personalRepository;
    private ProjectRepository projectRepository;
    private GitCloner gitCloner;

    @BeforeEach
    void setUp() {
        this.gitDiffAnalyzer = mock(GitDiffAnalyzer.class);
        this.myUserRepository = mock(MyUserRepository.class);
        this.gitPuller = mock(GitPuller.class);
        this.personalRepository = mock(PersonalRepository.class);
        this.projectRepository = mock(ProjectRepository.class);

        // 先建立出實體，在 spy
        this.gitCloner = new GitCloner(gitDiffAnalyzer, myUserRepository, gitPuller, personalRepository, projectRepository);
        this.gitCloner = spy(gitCloner);
    }

    @Nested
    class cloneRepositoryTest {
        String repoUrl = "https://github.com/ruserxd/test.git";
        String commitId = "587a0e12610554a97b3aea6d6126ed92fb010865";
        Long userId = 1L;

        @BeforeEach
        void setUp() {
            repoUrl = "https://github.com/ruserxd/test.git";
            commitId = "587a0e12610554a97b3aea6d6126ed92fb010865";
            userId = 1L;
            when(myUserRepository.findByUserId(userId))
                    .thenReturn(Optional.ofNullable(MyUser.builder()
                                                          .userId(1L)
                                                          .build()));
        }

        @Test
        @DisplayName("測試拋出 user 已經有 clone 過的例外")
        void userHadClonedTest() throws GitAPIException, IOException {
            try (MockedStatic<GitFunction> mockedStatic = mockStatic(GitFunction.class)) {
                mockedStatic.when(() -> GitFunction.isUserCloned(anyLong(),               // userId
                                    any(RepositoryINFO.class),  // repoINFO
                                    any(PersonalRepository.class)))
                            .thenReturn(true);
                GitResult result = gitCloner.cloneRepository(repoUrl, commitId, userId);
                GitResult excepted = GitResult.builder()
                                              .message("此帳戶已經有 clone 過 " + repoUrl)
                                              .status(GitStatus.CLONE_FAILED)
                                              .build();
                assertEquals(excepted, result);
            }
        }

        @Test
        @DisplayName("測試本地端有該存儲庫，但 pull 失敗的情況")
        void pullWrongTest() throws GitAPIException, IOException {
            try (MockedStatic<GitFunction> mockedStatic = mockStatic(GitFunction.class)) {
                mockedStatic.when(() -> GitFunction.getRepoNameFromUrl(repoUrl))
                            .thenReturn("test");

                mockedStatic.when(() -> GitFunction.isUserCloned(anyLong(),               // userId
                                    any(RepositoryINFO.class),  // repoINFO
                                    any(PersonalRepository.class)))
                            .thenReturn(false);
                mockedStatic.when(() -> GitFunction.isLocalCloned(anyString()))
                            .thenReturn(true);

                GitResult gitResult = GitResult.builder()
                                               .status(GitStatus.PULL_FAILED)
                                               .build();
                when(gitPuller.pullLocalRepository(any(RepositoryINFO.class)))
                        .thenReturn(gitResult);

                // 跑到 pullAndUpdateDataBase
                GitResult result = gitCloner.cloneRepository(repoUrl, commitId, userId);

                GitResult excepted = GitResult.builder()
                                              .status(GitStatus.PULL_FAILED)
                                              .build();
                assertEquals(excepted, result);
            }
        }

        @Test
        @DisplayName("測試本地端有該存儲庫，但獲取資料庫的 Project 失敗的情況")
        void dataBaseWrongTest() throws GitAPIException, IOException {
            try (MockedStatic<GitFunction> mockedStatic = mockStatic(GitFunction.class)) {
                mockedStatic.when(() -> GitFunction.getRepoNameFromUrl(repoUrl))
                            .thenReturn("test");

                mockedStatic.when(() -> GitFunction.isUserCloned(anyLong(),               // userId
                                    any(RepositoryINFO.class),  // repoINFO
                                    any(PersonalRepository.class)))
                            .thenReturn(false);
                mockedStatic.when(() -> GitFunction.isLocalCloned(anyString()))
                            .thenReturn(true);

                GitResult gitResult = GitResult.builder()
                                               .status(GitStatus.PULL_SUCCESS)
                                               .build();
                when(gitPuller.pullLocalRepository(any(RepositoryINFO.class)))
                        .thenReturn(gitResult);

                when(projectRepository.findByProjectName(anyString()))
                        .thenReturn(Optional.empty());

                // 跑到 pullAndUpdateDataBase
                GitResult result = gitCloner.cloneRepository(repoUrl, commitId, userId);

                GitResult excepted = GitResult.builder()
                                              .status(GitStatus.DATABASE_FAILED)
                                              .message("資料庫獲取 project 時失敗")
                                              .build();
                assertEquals(excepted, result);
            }
        }

        @Test
        @DisplayName("測試本地端有該存儲庫，但讀取 repository 時發生錯誤")
        void getLocalRepoWrongTest() throws GitAPIException, IOException {
            try (MockedStatic<GitFunction> mockedStatic = mockStatic(GitFunction.class);
                 // 攔截建構
                 MockedConstruction<FileRepository> fileRepoMock = Mockito.mockConstruction(
                         FileRepository.class,
                         (mock, context) -> {
                             // 當呼叫 .resolve 時，拋出例外
                             when(mock.resolve(anyString()))
                                     .thenThrow(new IOException("Repository path does not exist"));
                         }
                 )) {
                mockedStatic.when(() -> GitFunction.getRepoNameFromUrl(repoUrl))
                            .thenReturn("test");

                mockedStatic.when(() -> GitFunction.isUserCloned(anyLong(),               // userId
                                    any(RepositoryINFO.class),  // repoINFO
                                    any(PersonalRepository.class)))
                            .thenReturn(false);
                mockedStatic.when(() -> GitFunction.isLocalCloned(anyString()))
                            .thenReturn(true);

                GitResult gitResult = GitResult.builder()
                                               .status(GitStatus.PULL_SUCCESS)
                                               .build();
                when(gitPuller.pullLocalRepository(any(RepositoryINFO.class)))
                        .thenReturn(gitResult);

                Project project = Project.builder()
                                         .projectName("test")
                                         .build();
                when(projectRepository.findByProjectName(anyString()))
                        .thenReturn(Optional.ofNullable(project));

                // 跑到 pullAndUpdateDataBase
                GitResult result = gitCloner.cloneRepository(repoUrl, commitId, userId);

                GitResult excepted = GitResult.builder()
                                              .status(GitStatus.CLONE_FAILED)
                                              .message("讀取 repository 時發生錯誤: Repository path does not exist")
                                              .build();

                assertEquals(excepted, result);
            }
        }


        @Test
        @DisplayName("測試本地端有該存儲庫，但讀取 objectId 時出現 NULL")
        void getObjectIdWrongTest() throws GitAPIException, IOException {
            try (MockedStatic<GitFunction> mockedStatic = mockStatic(GitFunction.class);
                 MockedConstruction<FileRepository> fileRepoMock = Mockito.mockConstruction(
                         FileRepository.class,
                         (mock, context) -> {
                             // 當呼叫 resolve 時回傳 null
                             when(mock.resolve(anyString()))
                                     .thenReturn(null);
                         }
                 )) {
                mockedStatic.when(() -> GitFunction.getRepoNameFromUrl(repoUrl))
                            .thenReturn("test");

                mockedStatic.when(() -> GitFunction.isUserCloned(anyLong(),               // userId
                                    any(RepositoryINFO.class),
                                    any(PersonalRepository.class)))
                            .thenReturn(false);
                mockedStatic.when(() -> GitFunction.isLocalCloned(anyString()))
                            .thenReturn(true);

                GitResult gitResult = GitResult.builder()
                                               .status(GitStatus.PULL_SUCCESS)
                                               .build();
                when(gitPuller.pullLocalRepository(any(RepositoryINFO.class)))
                        .thenReturn(gitResult);

                Project project = Project.builder()
                                         .projectName("test")
                                         .build();
                when(projectRepository.findByProjectName(anyString()))
                        .thenReturn(Optional.ofNullable(project));

                // 跑到 pullAndUpdateDataBase
                GitResult result = gitCloner.cloneRepository(repoUrl, commitId, userId);

                GitResult excepted = GitResult.builder()
                                              .status(GitStatus.CLONE_FAILED)
                                              .message("無法獲取正確的 commit SHA1")
                                              .build();

                assertEquals(excepted, result);
            }
        }

        @Test
        @DisplayName("測試本地端有該存儲庫，正確的更新資料庫")
        void pullAndUpdateDataBaseTest() throws GitAPIException, IOException {
            try (MockedStatic<GitFunction> mockedStatic = mockStatic(GitFunction.class);
                 MockedConstruction<FileRepository> fileRepoMock = mockConstruction(
                         FileRepository.class,
                         (mock, context) -> {
                             when(mock.resolve(anyString()))
                                     .thenReturn(ObjectId.fromString("1234567890123456789012345678901234567890"));
                         }
                 )) {
                mockedStatic.when(() -> GitFunction.getRepoNameFromUrl(repoUrl))
                            .thenReturn("test");

                mockedStatic.when(() -> GitFunction.isUserCloned(anyLong(),               // userId
                                    any(RepositoryINFO.class),
                                    any(PersonalRepository.class)))
                            .thenReturn(false);
                mockedStatic.when(() -> GitFunction.isLocalCloned(anyString()))
                            .thenReturn(true);

                GitResult gitResult = GitResult.builder()
                                               .status(GitStatus.PULL_SUCCESS)
                                               .build();
                when(gitPuller.pullLocalRepository(any(RepositoryINFO.class)))
                        .thenReturn(gitResult);

                Project project = Project.builder()
                                         .projectName("test")
                                         .build();
                when(projectRepository.findByProjectName(anyString()))
                        .thenReturn(Optional.ofNullable(project));

                // 跑到 pullAndUpdateDataBase
                GitResult result = gitCloner.cloneRepository(repoUrl, commitId, userId);

                GitResult excepted = GitResult.builder()
                                              .status(GitStatus.PULL_SUCCESS)
                                              .message("因為本地端有該存儲庫，因此改為 Pull 並成功 Pull 更新資料")
                                              .build();

                assertEquals(excepted, result);
            }
        }

        @Test
        @DisplayName("測試本地端有該存儲庫，正確的更新資料庫，commitID 為 HEAD")
        void commitIDisHEADTest() throws GitAPIException, IOException {
            try (MockedStatic<GitFunction> mockedStatic = mockStatic(GitFunction.class);
                 MockedConstruction<FileRepository> mocked = mockConstruction(
                         FileRepository.class,
                         (mock, context) -> {
                             when(mock.resolve(anyString()))
                                     .thenReturn(ObjectId.fromString("1234567890123456789012345678901234567890"));
                         }
                 )) {
                commitId = "HEAD";
                mockedStatic.when(() -> GitFunction.getRepoNameFromUrl(repoUrl))
                            .thenReturn("test");

                mockedStatic.when(() -> GitFunction.isUserCloned(anyLong(),               // userId
                                    any(RepositoryINFO.class),
                                    any(PersonalRepository.class)))
                            .thenReturn(false);
                mockedStatic.when(() -> GitFunction.isLocalCloned(anyString()))
                            .thenReturn(true);

                GitResult gitResult = GitResult.builder()
                                               .status(GitStatus.PULL_SUCCESS)
                                               .build();
                when(gitPuller.pullLocalRepository(any(RepositoryINFO.class)))
                        .thenReturn(gitResult);

                Project project = Project.builder()
                                         .projectName("test")
                                         .build();
                when(projectRepository.findByProjectName(anyString()))
                        .thenReturn(Optional.ofNullable(project));

                // 跑到 pullAndUpdateDataBase
                GitResult result = gitCloner.cloneRepository(repoUrl, commitId, userId);

                GitResult excepted = GitResult.builder()
                                              .status(GitStatus.PULL_SUCCESS)
                                              .message("因為本地端有該存儲庫，因此改為 Pull 並成功 Pull 更新資料")
                                              .build();

                assertEquals(excepted, result);
            }
        }

        @Test
        @DisplayName("測試本地端未有該存儲庫，clone並成功分析檔案")
        void cloneAndAnalyzedIsHEADTest() throws GitAPIException, IOException {
            commitId = "HEAD";
            try (MockedStatic<GitFunction> gitFunctionMocked = mockStatic(GitFunction.class);
                 MockedStatic<Git> gitMocked = mockStatic(Git.class)) {
                // 模擬 GitFunction
                gitFunctionMocked.when(() -> GitFunction.getRepoNameFromUrl(repoUrl))
                                 .thenReturn("test");

                gitFunctionMocked.when(() -> GitFunction.isUserCloned(anyLong(),               // userId
                                         any(RepositoryINFO.class),  // repoINFO
                                         any(PersonalRepository.class)))
                                 .thenReturn(false);
                gitFunctionMocked.when(() -> GitFunction.isLocalCloned(anyString()))
                                 .thenReturn(false);

                // 模擬 Git
                Git mockGit = mock(Git.class);
                CloneCommand mockCloneCommand = mock(CloneCommand.class);
                when(mockCloneCommand.setURI(anyString())).thenReturn(mockCloneCommand);
                when(mockCloneCommand.setDirectory(any(File.class))).thenReturn(mockCloneCommand);
                when(mockCloneCommand.call()).thenReturn(mockGit);

                when(Git.cloneRepository()).thenReturn(mockCloneCommand);

                // 執行分析並獲取回傳資料
                GitResult excepted = GitResult.builder()
                                              .status(GitStatus.CLONE_SUCCESS)
                                              .message("Test Success")
                                              .build();
                when(gitDiffAnalyzer.analyzeAllCommits(anyString(), any(MyUser.class)))
                        .thenReturn(excepted);

                // 執行測試
                GitResult result = gitCloner.cloneRepository(repoUrl, commitId, userId);

                assertEquals(excepted, result);
            }
        }

        @Test
        @DisplayName("測試本地端未有該存儲庫，clone並成功分析檔案 (commitId 非 HEAD)")
        void cloneAndAnalyzedNotHEADTest() throws GitAPIException, IOException {
            commitId = "1234567890123456789012345678901234567890";
            try (MockedStatic<GitFunction> gitFunctionMocked = mockStatic(GitFunction.class);
                 MockedStatic<Git> gitMocked = mockStatic(Git.class)) {
                // 模擬 GitFunction
                gitFunctionMocked.when(() -> GitFunction.getRepoNameFromUrl(repoUrl))
                                 .thenReturn("test");

                gitFunctionMocked.when(() -> GitFunction.isUserCloned(anyLong(),               // userId
                                         any(RepositoryINFO.class),  // repoINFO
                                         any(PersonalRepository.class)))
                                 .thenReturn(false);
                gitFunctionMocked.when(() -> GitFunction.isLocalCloned(anyString()))
                                 .thenReturn(false);

                // 模擬 Git
                Git mockGit = mock(Git.class);
                CloneCommand mockCloneCommand = mock(CloneCommand.class);
                when(mockCloneCommand.setURI(anyString())).thenReturn(mockCloneCommand);
                when(mockCloneCommand.setDirectory(any(File.class))).thenReturn(mockCloneCommand);
                when(mockCloneCommand.call()).thenReturn(mockGit);

                when(Git.cloneRepository()).thenReturn(mockCloneCommand);

                // 跳過 checkToCommitId
                doNothing().when(gitCloner).checkToCommitId(any(Git.class), anyString());

                // 執行分析並獲取回傳資料
                GitResult excepted = GitResult.builder()
                                              .status(GitStatus.CLONE_SUCCESS)
                                              .message("Test Success")
                                              .build();
                when(gitDiffAnalyzer.analyzeAllCommits(anyString(), any(MyUser.class)))
                        .thenReturn(excepted);

                // 執行測試
                GitResult result = gitCloner.cloneRepository(repoUrl, commitId, userId);

                assertEquals(excepted, result);
            }
        }

        @Test
        @DisplayName("測試本地端未有該存儲庫，command.call()拋出 GitAPIException")
        void commandCallExceptionTest() throws GitAPIException, IOException {
            commitId = "HEAD";
            try (MockedStatic<GitFunction> gitFunctionMocked = mockStatic(GitFunction.class);
                 MockedStatic<Git> gitMocked = mockStatic(Git.class)) {
                // 模擬 GitFunction
                gitFunctionMocked.when(() -> GitFunction.getRepoNameFromUrl(repoUrl))
                                 .thenReturn("test");

                gitFunctionMocked.when(() -> GitFunction.isUserCloned(anyLong(),               // userId
                                         any(RepositoryINFO.class),  // repoINFO
                                         any(PersonalRepository.class)))
                                 .thenReturn(false);
                gitFunctionMocked.when(() -> GitFunction.isLocalCloned(anyString()))
                                 .thenReturn(false);

                // 模擬 Git
                Git mockGit = mock(Git.class);
                CloneCommand mockCloneCommand = mock(CloneCommand.class);
                when(mockCloneCommand.setURI(anyString())).thenReturn(mockCloneCommand);
                when(mockCloneCommand.setDirectory(any(File.class))).thenReturn(mockCloneCommand);
                when(mockCloneCommand.call()).thenThrow(new GitAPIException("clone failed") {
                });

                when(Git.cloneRepository()).thenReturn(mockCloneCommand);

                // 跳過 checkToCommitId
                doNothing().when(gitCloner).checkToCommitId(any(Git.class), anyString());

                // 執行測試
                GitResult excepted = GitResult.builder()
                                              .status(GitStatus.CLONE_FAILED)
                                              .message("Clone 發生 clone failed")
                                              .build();
                GitResult result = gitCloner.cloneRepository(repoUrl, commitId, userId);
                assertEquals(excepted, result);
            }
        }

        @Test
        @DisplayName("測試本地端未有該存儲庫，執行 checkToCommitId，commitId 格式不對拋出 RevisionSyntaxException")
        void commitIdWrongTest() throws GitAPIException, IOException {
            try (MockedStatic<GitFunction> gitFunctionMocked = mockStatic(GitFunction.class);
                 MockedStatic<Git> gitMocked = mockStatic(Git.class)) {
                // 模擬 GitFunction
                gitFunctionMocked.when(() -> GitFunction.getRepoNameFromUrl(repoUrl))
                                 .thenReturn("test");

                gitFunctionMocked.when(() -> GitFunction.isUserCloned(anyLong(),               // userId
                                         any(RepositoryINFO.class),  // repoINFO
                                         any(PersonalRepository.class)))
                                 .thenReturn(false);
                gitFunctionMocked.when(() -> GitFunction.isLocalCloned(anyString()))
                                 .thenReturn(false);

                // 模擬 Git
                Git mockGit = mock(Git.class);
                CloneCommand mockCloneCommand = mock(CloneCommand.class);
                when(mockCloneCommand.setURI(anyString())).thenReturn(mockCloneCommand);
                when(mockCloneCommand.setDirectory(any(File.class))).thenReturn(mockCloneCommand);
                when(mockCloneCommand.call()).thenReturn(mockGit);

                when(Git.cloneRepository()).thenReturn(mockCloneCommand);

                // checkToCommitId 拋例外
                RevisionSyntaxException exception = new RevisionSyntaxException("error");
                doThrow(exception)
                        .when(gitCloner).checkToCommitId(any(Git.class), anyString());

                // 執行分析並獲取回傳資料
                String res = "Clone 發生 " + exception.getMessage();
                GitResult excepted = GitResult.builder()
                                              .status(GitStatus.CLONE_FAILED)
                                              .message(res)
                                              .build();
                when(gitDiffAnalyzer.analyzeAllCommits(anyString(), any(MyUser.class)))
                        .thenReturn(excepted);

                // 執行測試
                GitResult result = gitCloner.cloneRepository(repoUrl, commitId, userId);

                assertEquals(excepted, result);
            }
        }
    }

    @Nested
    class checkToCommitIdTest {
        Git git;
        Repository repository;
        String commitId;

        @BeforeEach
        void setUp() {
            git = mock(Git.class);
            repository = mock(Repository.class);
            commitId = "587a0e12610554a97b3aea6d6126ed92fb010865";
        }

        @Test
        @DisplayName("測試當獲取 commitId 發生錯誤的拋出")
        void throwGitAPIExceptionTest() throws IOException {
            when(git.getRepository()).thenReturn(repository);
            when(repository.resolve(anyString()))
                    .thenThrow(new IOException("路徑錯誤"));
            IOException accurate = assertThrows(IOException.class, () -> gitCloner.checkToCommitId(git, commitId));
            assertEquals("路徑錯誤", accurate.getMessage());
        }

        @Test
        @DisplayName("測試當獲取 commitId 為 null 的拋出")
        void getCommitIsNullTest() throws IOException {
            when(git.getRepository()).thenReturn(repository);
            when(repository.resolve(anyString()))
                    .thenReturn(null);
            RevisionSyntaxException accurate = assertThrows(RevisionSyntaxException.class, () -> gitCloner.checkToCommitId(git, commitId));
            assertNull(accurate.getMessage());
        }

        @Test
        @DisplayName("測試正常的切換 commitId")
        void checkCommitIdTest() throws IOException, GitAPIException {
            when(git.getRepository()).thenReturn(repository);
            when(repository.resolve(anyString()))
                    .thenReturn(ObjectId.fromString("1234567890123456789012345678901234567890"));
            CheckoutCommand checkoutCommand = mock(CheckoutCommand.class);

            // Method Chaining 方法鏈
            when(git.checkout()).thenReturn(checkoutCommand);
            when(checkoutCommand.setName(anyString())).thenReturn(checkoutCommand);
            when(checkoutCommand.call()).thenReturn(null);

            gitCloner.checkToCommitId(git, commitId);
        }
    }

}