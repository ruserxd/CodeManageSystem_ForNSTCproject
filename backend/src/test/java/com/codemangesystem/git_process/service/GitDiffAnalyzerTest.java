package com.codemangesystem.git_process.service;

import com.codemangesystem.git_process.model.database.DiffInfo;
import com.codemangesystem.git_process.model.database.Files;
import com.codemangesystem.git_process.model.database.Method;
import com.codemangesystem.git_process.model.database.Project;
import com.codemangesystem.git_process.model.git.GitResult;
import com.codemangesystem.git_process.model.git.GitStatus;
import com.codemangesystem.git_process.repository.PersonalRepository;
import com.codemangesystem.git_process.repository.ProjectRepository;
import com.codemangesystem.login_process.model.user.MyUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static com.codemangesystem.FileReader.getFileCode;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
class GitDiffAnalyzerTest {
    ProjectRepository projectRepository;
    PersonalRepository personalRepository;
    GitDiffAnalyzer gitDiffAnalyzer;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        personalRepository = mock(PersonalRepository.class);
        gitDiffAnalyzer = new GitDiffAnalyzer(projectRepository, personalRepository);
    }

    @Nested
    class analyzeAllCommitsTest {
        MyUser user = MyUser.builder()
                            .userId(1L)
                            .build();
        @TempDir
        Path tempDir;

        @Test
        @DisplayName("沒有此路徑的情況")
        void noDirectoryTest() throws GitAPIException, IOException {
            // 測試
            GitResult actual = gitDiffAnalyzer.analyzeAllCommits("", user);

            // 驗證
            assertEquals("分析時，本地端沒有此檔案", actual.getMessage());
            assertEquals(GitStatus.ANALYSIS_FAILED, actual.getStatus());
        }

        @Test
        @DisplayName("有檔案，但沒 commit 過")
        void noCommitTest() throws IOException, GitAPIException {
            // 資料初始化
            // 新增一個臨時的路徑出來
            // 有 .git 但沒有 commit 紀錄
            Path repoPath = tempDir.resolve("test-noCommit");
            java.nio.file.Files.createDirectory(repoPath);
            Path gitDir = repoPath.resolve(".git");
            java.nio.file.Files.createDirectory(gitDir);

            // 執行測試
            GitResult actual = gitDiffAnalyzer.analyzeAllCommits(repoPath.toString(), user);

            // 驗證
            assertEquals("分析時，此資料沒有 commit 的紀錄", actual.getMessage());
            assertEquals(GitStatus.ANALYSIS_FAILED, actual.getStatus());
        }

        @Test
        @DisplayName("測試有 .git 但沒有資料可進行分析")
        void noJavaFileAnalyzedTest() throws IOException, GitAPIException {
            Path repoPath = tempDir.resolve("test-noCommit");
            java.nio.file.Files.createDirectory(repoPath);

            // git init
            Git git = Git.init().setDirectory(repoPath.toFile()).call();

            // git commit 一個空的 message
            git.commit().setMessage("test").setAllowEmpty(true).call();

            // 執行測試
            GitResult actual = gitDiffAnalyzer.analyzeAllCommits(repoPath.toString(), user);

            // 驗證
            assertEquals("未出現可分析方法，可能是沒有方法可以分類", actual.getMessage());
            assertEquals(GitStatus.CLONE_SUCCESS, actual.getStatus());
        }

        @Test
        @DisplayName("完整分析")
        void successAnalyze() throws IOException, GitAPIException {
            Path repoPath = tempDir.resolve("test-noCommit");
            java.nio.file.Files.createDirectory(repoPath);

            // git init
            Git git = Git.init().setDirectory(repoPath.toFile()).call();

            // 創建 java 檔案
            Path javaFile = repoPath.resolve("test.java");
            java.nio.file.Files.write(javaFile, """
                    public class Tester {
                        public void test() {
                            System.out.println("Hello");
                        }
                    }
                    """.getBytes());

            // git add + git commit
            git.add().addFilepattern("test.java").call();
            git.commit().setMessage("test-1").call();


            // 修改 java 檔案
            java.nio.file.Files.write(javaFile, """
                    public class Tester {
                        public void test() {
                            System.out.println("Hello, World!");
                        }
                    }
                    """.getBytes());

            // git add + git commit
            git.add().addFilepattern("test.java").call();
            git.commit().setMessage("test-2").call();

            // 執行測試
            GitResult actual = gitDiffAnalyzer.analyzeAllCommits(repoPath.toString(), user);

            // 驗證
            assertEquals("成功將資料分類完成", actual.getMessage());
            assertEquals(GitStatus.CLONE_SUCCESS, actual.getStatus());
        }

        @Test
        @DisplayName("拋出 IOException")
        void throwIOExceptionTest() throws GitAPIException, IOException, IllegalAccessException {
            // 這邊為了測試拋出需要 spy，當呼叫 getCommitDiffList 拋出例外給此方法
            GitDiffAnalyzer spyGitDiffAnalyzer = spy(new GitDiffAnalyzer(projectRepository, personalRepository));

            Path repoPath = tempDir.resolve("test-noCommit");
            java.nio.file.Files.createDirectory(repoPath);

            // git init
            Git git = Git.init().setDirectory(repoPath.toFile()).call();

            // 創建 java 檔案
            Path javaFile = repoPath.resolve("test.java");
            java.nio.file.Files.write(javaFile, """
                    public class Tester {
                        public void test() {
                            System.out.println("Hello");
                        }
                    }
                    """.getBytes());

            // git add + git commit
            git.add().addFilepattern("test.java").call();
            git.commit().setMessage("test-1").call();

            doThrow(new IOException("getCommit error"))
                    .when(spyGitDiffAnalyzer).getCommitDiffList(any(), any(), any(), any());

            // 執行測試
            GitResult actual = spyGitDiffAnalyzer.analyzeAllCommits(repoPath.toString(), user);

            // 驗證
            assertEquals("分析時，發生 getCommit error", actual.getMessage());
            assertEquals(GitStatus.ANALYSIS_FAILED, actual.getStatus());
        }

        @Test
        @DisplayName("拋出 GitAPIException")
        void throwGitAPIExceptionTest() throws GitAPIException, IOException {
            // 這邊為了測試拋出需要 spy，當呼叫 getCommitDiffList 拋出例外給此方法
            GitDiffAnalyzer spyGitDiffAnalyzer = spy(new GitDiffAnalyzer(projectRepository, personalRepository));

            Path repoPath = tempDir.resolve("test-noCommit");
            java.nio.file.Files.createDirectory(repoPath);

            // git init
            Git git = Git.init().setDirectory(repoPath.toFile()).call();

            // 創建 java 檔案
            Path javaFile = repoPath.resolve("test.java");
            java.nio.file.Files.write(javaFile, """
                    public class Tester {
                        public void test() {
                            System.out.println("Hello");
                        }
                    }
                    """.getBytes());

            // git add + git commit
            git.add().addFilepattern("test.java").call();
            git.commit().setMessage("test-1").call();

            // 模擬 LogCommand
            LogCommand mockLogCommand = mock(LogCommand.class);
            when(mockLogCommand.call()).thenThrow(new GitAPIException("getCommit error") {
            });

            try (MockedConstruction<Git> gitMockedConstruction =
                         mockConstruction(Git.class, (mock, context) -> {
                             // 模擬 log() 方法返回我們的 mock LogCommand
                             when(mock.log()).thenReturn(mockLogCommand);
                         })) {

                // 執行測試
                GitResult actual = spyGitDiffAnalyzer.analyzeAllCommits(repoPath.toString(), user);

                // 驗證結果
                assertEquals("分析時，發生 getCommit error", actual.getMessage());
                assertEquals(GitStatus.ANALYSIS_FAILED, actual.getStatus());

                // 驗證方法調用
                Git constructedGit = gitMockedConstruction.constructed().getFirst();
                verify(constructedGit).log();
                verify(mockLogCommand).call();
            }
        }
    }

    @Nested
    class analyzePartCommitsTest {
        @TempDir
        Path tempDir;

        String repoPath;
        String oldHeadRevStr;

        @BeforeEach
        void setUp() {
            repoPath = tempDir.toString();
            oldHeadRevStr = "7a824f197138d7a27689ce56ec732b2bafa3684d";
        }

        @Test
        @DisplayName("不存在該檔案")
        void noDirectoryTest() throws IOException {
            // 測試
            GitResult actual = gitDiffAnalyzer.analyzePartCommits(repoPath, oldHeadRevStr);
            log.info(String.valueOf(actual));

            // 驗證
            assertEquals("部分分析時，本地端沒有此檔案", actual.getMessage());
            assertEquals(GitStatus.ANALYSIS_FAILED, actual.getStatus());
        }

        @Test
        @DisplayName("沒有 project 的情況")
        void noProjectTest() throws GitAPIException, IOException {
            Path repoPath = tempDir.resolve("test");

            // git init
            Git git = Git.init().setDirectory(repoPath.toFile()).call();

            when(projectRepository.findByProjectName(anyString())).thenReturn(Optional.empty());

            // 測試
            GitResult actual = gitDiffAnalyzer.analyzePartCommits(String.valueOf(repoPath), oldHeadRevStr);
            log.info(String.valueOf(actual));

            // 驗證
            assertEquals("資料庫沒有此檔案", actual.getMessage());
            assertEquals(GitStatus.ANALYSIS_FAILED, actual.getStatus());
        }

        @Test
        @DisplayName("測試完整的部分分析，並且有中斷點")
        void successAnalyze() throws GitAPIException, IOException {
            Path repoPath = tempDir.resolve("test");

            // git init
            Git git = Git.init().setDirectory(repoPath.toFile()).call();

            // 抓資料庫的 project
            Project project = Project.builder().build();
            when(projectRepository.findByProjectName(anyString())).thenReturn(Optional.ofNullable(project));

            // 寫入兩個 commit
            Path javaFile = tempDir.resolve("tester.java");
            java.nio.file.Files.write(javaFile, """
                    public class Tester {
                        public void test() {
                            System.out.println("Hello");
                        }
                    }
                    """.getBytes());

            // git add + git commit
            git.add().addFilepattern("tester.java").call();
            git.commit().setMessage("test-1").call();

            // 獲取第一次 commit 的 RevStr
            // 為了測試到 if (commit.getName().equals(oldHeadRevstr))
            RevCommit firstCommit = git.log().setMaxCount(1).call().iterator().next();
            oldHeadRevStr = firstCommit.getName();
            log.info("獲得第一個 HeadRevstr {}", oldHeadRevStr);

            java.nio.file.Files.write(javaFile, """
                    public class Tester {
                        public void test() {
                            System.out.println("Hello, World!");
                        }
                    }
                    """.getBytes());

            // git add + git commit
            git.add().addFilepattern("tester.java").call();
            git.commit().setMessage("test-2").call();

            // 測試
            GitResult actual = gitDiffAnalyzer.analyzePartCommits(String.valueOf(repoPath), oldHeadRevStr);
            log.info(String.valueOf(actual));

            // 驗證
            assertEquals("成功 Pull 並更新資料", actual.getMessage());
            assertEquals(GitStatus.PULL_SUCCESS, actual.getStatus());
        }

        @Test
        @DisplayName("測試完整的部分分析，沒有中斷點")
        void successAnalyze2() throws GitAPIException, IOException {
            Path repoPath = tempDir.resolve("test");

            // git init
            Git git = Git.init().setDirectory(repoPath.toFile()).call();

            // 抓資料庫的 project
            Project project = Project.builder().build();
            when(projectRepository.findByProjectName(anyString())).thenReturn(Optional.ofNullable(project));

            // 寫入兩個 commit
            Path javaFile = tempDir.resolve("tester.java");
            java.nio.file.Files.write(javaFile, """
                    public class Tester {
                        public void test() {
                            System.out.println("Hello");
                        }
                    }
                    """.getBytes());

            // git add + git commit
            git.add().addFilepattern("tester.java").call();
            git.commit().setMessage("test-1").call();

            java.nio.file.Files.write(javaFile, """
                    public class Tester {
                        public void test() {
                            System.out.println("Hello, World!");
                        }
                    }
                    """.getBytes());

            // git add + git commit
            git.add().addFilepattern("tester.java").call();
            git.commit().setMessage("test-2").call();

            // 測試
            GitResult actual = gitDiffAnalyzer.analyzePartCommits(String.valueOf(repoPath), oldHeadRevStr);
            log.info(String.valueOf(actual));

            // 驗證
            assertEquals("成功 Pull 並更新資料", actual.getMessage());
            assertEquals(GitStatus.PULL_SUCCESS, actual.getStatus());
        }

        @Test
        @DisplayName("拋出 IOException")
        void throwIOExceptionTest() throws IOException, GitAPIException {
            // 這邊為了測試拋出需要 spy，當呼叫 getCommitDiffList 拋出例外給此方法
            GitDiffAnalyzer spyGitDiffAnalyzer = spy(new GitDiffAnalyzer(projectRepository, personalRepository));

            Path repoPath = tempDir.resolve("test-noCommit");
            java.nio.file.Files.createDirectory(repoPath);

            // 抓資料庫的 project
            Project project = Project.builder().build();
            when(projectRepository.findByProjectName(anyString())).thenReturn(Optional.ofNullable(project));

            // git init
            Git git = Git.init().setDirectory(repoPath.toFile()).call();

            // 創建 java 檔案
            Path javaFile = repoPath.resolve("test.java");
            java.nio.file.Files.write(javaFile, """
                    public class Tester {
                        public void test() {
                            System.out.println("Hello");
                        }
                    }
                    """.getBytes());

            // git add + git commit
            git.add().addFilepattern("test.java").call();
            git.commit().setMessage("test-1").call();

            doThrow(new IOException("getCommit error"))
                    .when(spyGitDiffAnalyzer).getCommitDiffList(any(), any(), any(), any());

            // 執行測試
            GitResult actual = spyGitDiffAnalyzer.analyzePartCommits(repoPath.toString(), oldHeadRevStr);

            // 驗證
            assertEquals("部分分析時，發生 getCommit error", actual.getMessage());
            assertEquals(GitStatus.ANALYSIS_FAILED, actual.getStatus());
        }

        @Test
        @DisplayName("拋出 GitAPIException")
        void throwGitAPIExceptionTest() throws GitAPIException, IOException {
            // 這邊為了測試拋出需要 spy，當呼叫 getCommitDiffList 拋出例外給此方法
            GitDiffAnalyzer spyGitDiffAnalyzer = spy(new GitDiffAnalyzer(projectRepository, personalRepository));

            Path repoPath = tempDir.resolve("test-noCommit");
            java.nio.file.Files.createDirectory(repoPath);

            // 抓資料庫的 project
            Project project = Project.builder().build();
            when(projectRepository.findByProjectName(anyString())).thenReturn(Optional.ofNullable(project));

            // git init
            Git git = Git.init().setDirectory(repoPath.toFile()).call();

            // 創建 java 檔案
            Path javaFile = repoPath.resolve("test.java");
            java.nio.file.Files.write(javaFile, """
                    public class Tester {
                        public void test() {
                            System.out.println("Hello");
                        }
                    }
                    """.getBytes());

            // git add + git commit
            git.add().addFilepattern("test.java").call();
            git.commit().setMessage("test-1").call();

            // 模擬 LogCommand
            LogCommand mockLogCommand = mock(LogCommand.class);
            when(mockLogCommand.call()).thenThrow(new GitAPIException("getCommit error") {
            });

            try (MockedConstruction<Git> gitMockedConstruction =
                         mockConstruction(Git.class, (mock, context) -> {
                             // 模擬 log() 方法返回我們的 mock LogCommand
                             when(mock.log()).thenReturn(mockLogCommand);
                         })) {

                // 執行測試
                GitResult actual = spyGitDiffAnalyzer.analyzePartCommits(repoPath.toString(), oldHeadRevStr);

                // 驗證結果
                assertEquals("部分分析時，發生 getCommit error", actual.getMessage());
                assertEquals(GitStatus.ANALYSIS_FAILED, actual.getStatus());

                // 驗證方法調用
                Git constructedGit = gitMockedConstruction.constructed().getFirst();
                verify(constructedGit).log();
                verify(mockLogCommand).call();
            }
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class getCommitDiffListTest {
        @Mock
        RevCommit commit;

        @Mock
        Git git;

        @Mock
        Repository repository;

        @Mock
        RevCommit previousCommit;

        GitDiffAnalyzer spyGitDiffAnalyzer;

        @BeforeEach
        void setUp() {
            spyGitDiffAnalyzer = spy(gitDiffAnalyzer);
        }

        @Test
        void successGetDiffListTest() throws IOException, GitAPIException, IllegalAccessException {
            // 模擬 prepareTreeParser 方法
            AbstractTreeIterator mockOldTree = mock(AbstractTreeIterator.class);
            AbstractTreeIterator mockNewTree = mock(AbstractTreeIterator.class);

            doReturn(mockOldTree).when(spyGitDiffAnalyzer).prepareTreeParser(repository, previousCommit);
            doReturn(mockNewTree).when(spyGitDiffAnalyzer).prepareTreeParser(repository, commit);

            // 模擬 git.diff() 調用鏈
            DiffCommand diffCommand = mock(DiffCommand.class);
            when(git.diff()).thenReturn(diffCommand);
            when(diffCommand.setOldTree(mockOldTree)).thenReturn(diffCommand);
            when(diffCommand.setNewTree(mockNewTree)).thenReturn(diffCommand);

            List<DiffEntry> mockDiffEntries = Collections.emptyList();
            when(diffCommand.call()).thenReturn(mockDiffEntries);

            // 執行測試
            List<DiffEntry> result = spyGitDiffAnalyzer.getCommitDiffList(commit, git, repository, previousCommit);

            // 驗證
            assertNotNull(result);
            verify(spyGitDiffAnalyzer).prepareTreeParser(repository, previousCommit);
            verify(spyGitDiffAnalyzer).prepareTreeParser(repository, commit);
        }

        @Test
        void throwIOExceptionTest() throws IOException {
            doThrow(new IOException("get Commit Error")).when(spyGitDiffAnalyzer)
                                                        .prepareTreeParser(repository, previousCommit);

            // 執行測試
            IOException exception = assertThrows(IOException.class, () -> spyGitDiffAnalyzer.getCommitDiffList(commit, git, repository, previousCommit));

            // 驗證
            assertEquals("get Commit Error", exception.getMessage());
        }

        @Test
        void throwGitAPIExceptionTest() throws IOException, GitAPIException {
            // 模擬 prepareTreeParser 方法
            AbstractTreeIterator mockOldTree = mock(AbstractTreeIterator.class);
            AbstractTreeIterator mockNewTree = mock(AbstractTreeIterator.class);

            doReturn(mockOldTree).when(spyGitDiffAnalyzer).prepareTreeParser(repository, previousCommit);
            doReturn(mockNewTree).when(spyGitDiffAnalyzer).prepareTreeParser(repository, commit);

            // 模擬 git.diff() 調用鏈
            DiffCommand diffCommand = mock(DiffCommand.class);
            when(git.diff()).thenReturn(diffCommand);
            when(diffCommand.setOldTree(mockOldTree)).thenReturn(diffCommand);
            when(diffCommand.setNewTree(mockNewTree)).thenReturn(diffCommand);

            List<DiffEntry> mockDiffEntries = Collections.emptyList();
            when(diffCommand.call()).thenThrow(new GitAPIException("get Commit Git Error") {
            });

            // 執行測試
            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> spyGitDiffAnalyzer.getCommitDiffList(commit, git, repository, previousCommit));

            // 驗證
            assertEquals("get Commit Git Error", exception.getMessage());
        }
    }

    @ExtendWith(MockitoExtension.class)
    @Nested
    class setCommitDiffToProjectTest {
        @TempDir
        Path tempDir;

        @Mock
        Project project;

        @Mock
        Git git;

        @Mock
        RevCommit commit;

        @Mock
        RevCommit previousCommit;

        @BeforeEach
        void setUp() {
            gitDiffAnalyzer = new GitDiffAnalyzer(projectRepository, personalRepository);
        }

        @Test
        @DisplayName("正常獲取 diff 的情況")
        void successSetDiffTest() throws GitAPIException, IOException {
            // 運用 spy
            GitDiffAnalyzer spyGitDiffAnalyzer = spy(gitDiffAnalyzer);

            // 預設
            // List<DiffEntry>
            DiffEntry mockDiffEntry = mock(DiffEntry.class);
            when(mockDiffEntry.getNewPath()).thenReturn("test.java");
            DiffEntry mockDiffEntry2 = mock(DiffEntry.class);
            when(mockDiffEntry2.getNewPath()).thenReturn("test.txt");
            List<DiffEntry> mockDiffs = Arrays.asList(mockDiffEntry, mockDiffEntry2);

            // getContentByCommit 直接回傳 ""
            doReturn("").when(spyGitDiffAnalyzer).getContentByCommit(any(), anyString(), any());

            // 獲取方法間的比較差異資訊
            List<Pair<String, String>> diffList = new ArrayList();
            diffList.add(Pair.of("", ""));
            diffList.add(Pair.of("", "xxx"));
            doReturn(diffList).when(spyGitDiffAnalyzer).compareTwoContent(any(), any());

            // 設定回傳的 DiffInfo
            DiffInfo diffInfo = DiffInfo.builder()
                                        .diffCode("")
                                        .authorEmail("xxx@gmail.com")
                                        .author("re")
                                        .commitMessage("test")
                                        .build();
            doReturn(diffInfo).when(spyGitDiffAnalyzer).takeCommitINFO(any(RevCommit.class));
            doNothing().when(spyGitDiffAnalyzer)
                       .addDiffInfoInToProject(anyString(), anyString(), anyString(), any(DiffInfo.class), any(Project.class));

            spyGitDiffAnalyzer.setCommitDiffToProject(mockDiffs, project, git, commit, previousCommit);
        }

        @Test
        @DisplayName("測試 IOException (發現到 log 上的問題...)")
        void throwIOExceptionTest() throws IOException {
            // 初始化 GitDiffAnalyzer
            GitDiffAnalyzer spyGitDiffAnalyzer = spy(gitDiffAnalyzer);

            // 只模擬必要的 DiffEntry 行為
            DiffEntry mockDiffEntry = mock(DiffEntry.class);
            when(mockDiffEntry.getNewPath()).thenReturn("test.java");
            List<DiffEntry> mockDiffs = Collections.singletonList(mockDiffEntry);

            when(commit.getId()).thenReturn(ObjectId.fromString("0123456789012345678901234567890123456789"));
            // 使用 git 和 previousCommit 作為固定值，filePath 使用匹配器
            doThrow(new IOException("get Content Error"))
                    .when(spyGitDiffAnalyzer)
                    .getContentByCommit(any(Git.class), anyString(), any(RevCommit.class));

            // 執行測試並驗證異常
            IOException exception = assertThrows(
                    IOException.class,
                    () -> spyGitDiffAnalyzer.setCommitDiffToProject(mockDiffs, project, git, commit, previousCommit)
            );
            assertEquals("get Content Error", exception.getMessage());
        }
    }

    @Nested
    class getHeadSHA1Test {
        Repository repo;
        //正確的 commit hash（40位的十六進制字符串）
        private static final String COMMIT_HASH = "abc123def456abc123def456abc123def456abc1";

        @BeforeEach
        void setUp() throws IOException {
            repo = mock(Repository.class);
            ObjectId commit = ObjectId.fromString(COMMIT_HASH);
            when(repo.resolve(Constants.HEAD)).thenReturn(commit);
        }

        @Test
        @DisplayName("正常 getHeadSHA1")
        void test() throws IOException {
            String actual = gitDiffAnalyzer.getHeadSHA1(repo);
            assertEquals(COMMIT_HASH, actual);
        }

        @Test
        @DisplayName("getHeadSHA1 拋出 IOException")
        void resolveFailed() throws IOException {
            when(repo.resolve(Constants.HEAD)).thenThrow(new IOException("Cannot resolve HEAD"));
            IOException exception = assertThrows(IOException.class,
                    () -> gitDiffAnalyzer.getHeadSHA1(repo));
            assertEquals("Cannot resolve HEAD", exception.getMessage());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class prepareTreeParserTest {
        @Mock
        Repository repository;
        @Mock
        RevCommit commit;
        @Mock
        RevTree tree;
        @Mock
        ObjectReader reader;

        @Test
        @DisplayName("正常獲得 treeIterator")
        void shouldReturnTreeParser() throws IOException {
            try (MockedConstruction<CanonicalTreeParser> mockedConstruction =
                         mockConstruction(CanonicalTreeParser.class, (mock, context) -> {
                             // 模擬 CanonicalTreeParser 行為
                             doNothing().when(mock).reset(any(ObjectReader.class), any(AnyObjectId.class));
                         })) {

                when(commit.getTree()).thenReturn(tree);
                when(repository.newObjectReader()).thenReturn(reader);
                when(tree.getId()).thenReturn(mock(ObjectId.class));

                // 測試
                AbstractTreeIterator result = gitDiffAnalyzer.prepareTreeParser(repository, commit);

                // 驗證
                assertNotNull(result);
            }
        }

        @Test
        @DisplayName("拋出 IOException")
        void throwIOException() {
            try (MockedConstruction<CanonicalTreeParser> mockedConstruction =
                         mockConstruction(CanonicalTreeParser.class, (mock, context) -> {
                             // 模擬 CanonicalTreeParser 行為
                             doThrow(new IOException("tree error")).when(mock)
                                                                   .reset(any(ObjectReader.class), any(AnyObjectId.class));
                         })) {

                when(commit.getTree()).thenReturn(tree);
                when(repository.newObjectReader()).thenReturn(reader);
                when(tree.getId()).thenReturn(mock(ObjectId.class));

                // 測試
                IOException exception = assertThrows(IOException.class
                        , () -> gitDiffAnalyzer.prepareTreeParser(repository, commit));

                // 驗證
                assertEquals("Git 語法樹上出現問題: tree error", exception.getMessage());
            }
        }
    }

    @Nested
    class takeCommitINFOTest {
        RevCommit revCommit;
        PersonIdent author;

        @BeforeEach
        void setUp() {
            // 使用固定的時間進行測試
            Date date = new Date(100000000L);
            TimeZone timeZone = TimeZone.getTimeZone("UTC");

            // 建立 mock
            revCommit = mock(RevCommit.class);
            PersonIdent mockAuthor = new PersonIdent(
                    "Test Author",
                    "test@example.com",
                    date,
                    timeZone
            );

            // 設置 mock 行為
            when(revCommit.getAuthorIdent()).thenReturn(mockAuthor);
            when(revCommit.getFullMessage()).thenReturn("Test commit message");
            when(revCommit.getName()).thenReturn("abc123");
            when(revCommit.getCommitTime()).thenReturn(100000);
        }

        @Test
        @DisplayName("測試正常 diffInfo")
        void test() {
            // 測試
            DiffInfo info = gitDiffAnalyzer.takeCommitINFO(revCommit);

            // 驗證
            assertEquals("Test Author", info.getAuthor());
            assertEquals("test@example.com", info.getAuthorEmail());
            assertEquals("Test commit message", info.getCommitMessage());
            assertEquals("abc123", info.getHeadRevstr());
            assertEquals(new Date(100000000L), info.getCommitTime());
        }
    }

    /*
     * 模擬出 file 並對其進行 git 的操作*/
    @Nested
    class getContentByCommitTest {
        @Test
        @DisplayName("檢查提交的內容正確")
        void testMultipleCommits() throws Exception {
            Path tempDir = java.nio.file.Files.createTempDirectory("test-repo");

            try (Git git = Git.init().setDirectory(tempDir.toFile()).call()) {
                // 提交
                String firstContent = "test content";
                File testFile = new File(tempDir.toFile(), "test.java");
                java.nio.file.Files.write(testFile.toPath(), firstContent.getBytes());

                git.add().addFilepattern("test.java").call();
                RevCommit firstCommit = git.commit()
                                           .setMessage("First commit")
                                           .call();

                // 驗證
                String firstActualContent = gitDiffAnalyzer.getContentByCommit(git, "test.java", firstCommit);

                assertEquals(firstContent, firstActualContent);
            } finally {
                FileUtils.deleteDirectory(tempDir.toFile());
            }
        }

        @Test
        @DisplayName("測試 IOException 的情況")
        void testIOException() throws Exception {
            // 創建臨時的 repo
            Path tempDir = java.nio.file.Files.createTempDirectory("test-repo");

            try (Git git = Git.init().setDirectory(tempDir.toFile()).call()) {
                // 創建一個初始提交
                String testContent = "test content";
                File testFile = new File(tempDir.toFile(), "test.java");
                java.nio.file.Files.write(testFile.toPath(), testContent.getBytes());

                // add 至 git, commit 上去
                git.add().addFilepattern("test.java").call();
                RevCommit commit = git.commit()
                                      .setMessage("Initial commit")
                                      .call();

                // 刪除 .git 目錄來製造讀取錯誤
                FileUtils.deleteDirectory(new File(tempDir.toFile(), ".git"));

                // 拋出 IOException
                assertThrows(IOException.class, () ->
                        gitDiffAnalyzer.getContentByCommit(git, "test.java", commit));
            } finally {
                FileUtils.deleteDirectory(tempDir.toFile());
            }
        }

        @Test
        @DisplayName("當文件不存在時返回空字串")
        void testNonExistentFile() throws Exception {
            Path tempDir = java.nio.file.Files.createTempDirectory("test-repo");

            try (Git git = Git.init().setDirectory(tempDir.toFile()).call()) {
                RevCommit commit = git.commit()
                                      .setMessage("Empty commit")
                                      .setAllowEmpty(true)
                                      .call();

                String content = gitDiffAnalyzer.getContentByCommit(git, "nonexistent.java", commit);
                assertEquals("", content);
            } finally {
                FileUtils.deleteDirectory(tempDir.toFile());
            }
        }
    }

    @Nested
    @DisplayName("比對兩個檔案並產出結果")
    class compareTwoContentTest {
        String newContent;
        String oldContent;

        @BeforeEach
        void setUp() {

        }

        @Test
        @DisplayName("測試 file 間的比較結果")
        void test() {
            newContent = getFileCode("src/test/resources/compareTwoContentData/newContent.txt");
            oldContent = getFileCode("src/test/resources/compareTwoContentData/oldContent.txt");

            // 測試
            List<Pair<String, String>> actual = gitDiffAnalyzer.compareTwoContent(oldContent, newContent);

            // 驗證
            List<Pair<String, String>> expected = new ArrayList<>();
            expected.add(Pair.of("getName(): String",
                    """
                            @@ -1,3 +1,3 @@
                             public String getName() {
                            -    return "KUNWEIsoHandSome";
                            +    return this.name;
                             }"""));
            expected.add(Pair.of("setName(String): void",
                    """
                            @@ -1,3 +1,3 @@
                             public void setName(String name) {
                            -    return;
                            +    this.name = name;
                             }"""));
            expected.add(Pair.of("showWelcomeMessage(): void",
                    """
                            @@ -1,3 +1,4 @@
                             /*歡迎使用者的加入*/
                             public void showWelcomeMessage() {
                            +    System.out.println("Welcome " + this.name + " to this ARPG World");
                             }"""));
            expected.add(Pair.of("willDelete(): void",
                    """
                            @@ -1,3 +1,1 @@
                            -public void willDelete() {
                            -    // delete this function
                            -}
                            +"""));

            assertIterableEquals(expected, actual);
        }

        // TODO : 新增更多測試
    }

    @Nested
    class getMethodByContentTest {
        String content;

        @Test
        @DisplayName("沒 annotation 的測試")
        void sinTest() {
            // 初始資料
            content = getFileCode("src/test/resources/getMethodByContentData/content.txt");
            String[] expected = {"factorial(double): double", "main(String[]): void", "sin(double): double", "sin(double, int): double"};

            // 測試
            Map<String, String> actualResult = gitDiffAnalyzer.getMethodByContent(content);
            String[] actual = actualResult.keySet().toArray(new String[0]);

            // 驗證
            assertArrayEquals(expected, actual);
        }

        @Test
        @DisplayName("有 annotation 的測試")
        void annotationTest() {
            // 初始資料
            content = getFileCode("src/test/resources/getMethodByContentData/havaAnnotationContent.txt");
            String[] excepted = {"register(MyUser): ResponseEntity<SessionResponse>", "getProjectNames(String): ResponseEntity<?>",
                    "pullByProjectName(String): ResponseEntity<?>", "deleteDataByProjectName(String, String): String", "ApiController(GitCloner, DataBaseService, UserService, GitPuller)", "getFileDataByProjectName(String): ResponseEntity<Project>"
                    , "login(LoginINFO): ResponseEntity<SessionResponse>", "fetchRepository(String, String, String): ResponseEntity<?>", "addSuperAccount(): void"};

            // 測試
            Map<String, String> actualResult = gitDiffAnalyzer.getMethodByContent(content);
            actualResult.forEach((k, v) -> System.out.println(k + "\n" + v));

            String[] actual = actualResult.keySet().toArray(new String[0]);

            // 驗證
            assertArrayEquals(excepted, actual);
        }
        // TODO : 新增更多測試
    }

    @Nested
    class generateGitDiffTest {
        String oldMethod;
        String newMethod;

        @BeforeEach
        void setUp() {
            oldMethod = getFileCode("src/test/resources/generateGitDiffData/oldMethod.txt");
            newMethod = getFileCode("src/test/resources/generateGitDiffData/newMethod.txt");
        }

        @Test
        @DisplayName("修改 hello world 比較測試")
        void hellWorldDiffTest() {
            // 測試
            String actual = gitDiffAnalyzer.generateGitDiff(oldMethod, newMethod);
            log.info("\n{}", actual);

            // 驗證
            String expected = getFileCode("src/test/resources/generateGitDiffData/diffResult.txt");

            String[] actualLines = actual.split("\n");
            String[] expectedLines = expected.split("\n");
            assertArrayEquals(actualLines, expectedLines);
        }

        @Test
        @DisplayName("一樣的 code 進行 diff")
        void noDiffTest() {
            newMethod = getFileCode("src/test/resources/generateGitDiffData/oldMethod.txt");

            // 測試
            String actual = gitDiffAnalyzer.generateGitDiff(oldMethod, newMethod);
            log.info("\n{}", actual);

            // 驗證
            String expected = getFileCode("src/test/resources/generateGitDiffData/noDiffResult.txt");

            String[] actualLines = actual.split("\n");
            String[] expectedLines = expected.split("\n");
            assertArrayEquals(actualLines, expectedLines);
        }

        @Test
        @DisplayName("測試 SoftBetaTest hw1 Commit 46f1671")
        void softBetaTestHw1Test() {
            oldMethod = getFileCode("src/test/resources/generateGitDiffData/softbetaTestHw1_Old.txt");
            newMethod = getFileCode("src/test/resources/generateGitDiffData/softbetaTestHw1_New.txt");

            // 測試
            String actual = gitDiffAnalyzer.generateGitDiff(oldMethod, newMethod);
            log.info("\n{}", actual);
        }
    }

    @Nested
    class addDiffInfoInToProjectTest {
        String filePath;
        String fileName;
        String methodName;
        DiffInfo diffInfo;
        Project project;

        // 預設的 files
        Files existingFile;

        // 傳入值的預設
        @BeforeEach
        void setUp() {
            filePath = "/Test";
            fileName = "Test.java";
            methodName = "testMethod";
            diffInfo = DiffInfo.builder()
                               .diffInfoId(1L)
                               .author("ruserxd")
                               .authorEmail("test@email.com")
                               .diffCode("+ ssss")
                               .build();
            project = Project.builder()
                             .projectName("testProject")
                             .files(new ArrayList<>())
                             .build();

            // 預設的 files
            existingFile = Files.builder()
                                .fileName("Test.java")
                                .filePath("/Test")
                                .methods(new ArrayList<>())
                                .build();
            project.getFiles().add(existingFile);
            project = spy(project);
        }

        @Test
        @DisplayName("file 存在, method 不存在")
        void fileExistsButMethodNotTest() {
            // 測試
            gitDiffAnalyzer.addDiffInfoInToProject(filePath, fileName, methodName, diffInfo, project);

            verify(project, times(1)).getFiles();

            // 確認結果
            Files expectedFiles = null;
            for (Files files : project.getFiles()) {
                log.info("測試獲得的 files {}", files.getFileName());
                if (files.getFileName().equals(fileName)) {
                    expectedFiles = files;
                    break;
                }
            }
            assertNotNull(expectedFiles);

            List<Method> methodList = expectedFiles.getMethods();
            Method method = methodList.getFirst();
            assertEquals(method.getMethodName(), "testMethod");

            List<DiffInfo> diffInfoList = method.getDiffInfoList();
            DiffInfo diffInfo = diffInfoList.getFirst();
            assertEquals(diffInfo.getDiffCode(), "+ ssss");
        }

        @Test
        @DisplayName("file 存在, method 存在")
        void fileAndMethodExistTest() {
            // 預設資料

            // 新增存在的 method 確保 method 的迴圈有執行確定放入兩個 method
            Method existingMethod = Method.builder()
                                          .methodId(1L)
                                          .methodName("testMethod2")
                                          .diffInfoList(new ArrayList<>())
                                          .build();
            Method existingMethod2 = Method.builder()
                                           .methodId(1L)
                                           .methodName(methodName)
                                           .diffInfoList(new ArrayList<>())
                                           .build();
            existingFile.getMethods().add(existingMethod);
            existingFile.getMethods().add(existingMethod2);

            List<Files> filesList = new ArrayList<>();
            filesList.add(existingFile);

            when(project.getFiles()).thenReturn(filesList);

            // 測試
            gitDiffAnalyzer.addDiffInfoInToProject(filePath, fileName, methodName, diffInfo, project);

            verify(project, times(1)).getFiles();

            // 確認結果
            Files exceptFiles = null;
            for (Files files : project.getFiles()) {
                if (files.getFileName().equals(fileName)) {
                    exceptFiles = files;
                    break;
                }
            }
            assertNotNull(exceptFiles);
            List<Method> methodList = exceptFiles.getMethods();

            Method method = methodList.get(1);
            assertEquals(method.getMethodName(), "testMethod");

            List<DiffInfo> diffInfoList = method.getDiffInfoList();
            DiffInfo diffInfo = diffInfoList.getFirst();
            assertEquals(diffInfo.getDiffCode(), "+ ssss");
        }

        @Test
        @DisplayName("file 不存在, method 存在")
        void fileNotExistButMethodExistTest() {
            // 預設資料
            Files existingFile = Files.builder()
                                      .fileName("Test2.java")
                                      .filePath("/Test2")
                                      .methods(new ArrayList<>())
                                      .build();
            List<Files> filesList = new ArrayList<>();
            filesList.add(existingFile);
            when(project.getFiles()).thenReturn(filesList);

            // 測試
            gitDiffAnalyzer.addDiffInfoInToProject(filePath, fileName, methodName, diffInfo, project);
            // 第一次的 if 會失敗所以會執行 2 次
            verify(project, times(2)).getFiles();

            // 確認結果
            Files exceptFiles = null;
            for (Files files : project.getFiles()) {
                if (files.getFileName().equals(fileName)) {
                    exceptFiles = files;
                    break;
                }
            }
            assertNotNull(exceptFiles);
            List<Method> methodList = exceptFiles.getMethods();

            Method method = methodList.getFirst();
            assertEquals(method.getMethodName(), "testMethod");

            List<DiffInfo> diffInfoList = method.getDiffInfoList();
            DiffInfo diffInfo = diffInfoList.getFirst();
            assertEquals(diffInfo.getDiffCode(), "+ ssss");
        }
    }
}