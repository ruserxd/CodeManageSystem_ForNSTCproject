package com.codemangesystem.gitProcess.service;

import com.codemangesystem.gitProcess.model_DataBase.DiffInfo;
import com.codemangesystem.gitProcess.model_DataBase.Files;
import com.codemangesystem.gitProcess.model_DataBase.Method;
import com.codemangesystem.gitProcess.model_DataBase.Project;
import com.codemangesystem.gitProcess.repository.PersonalRepository;
import com.codemangesystem.gitProcess.repository.ProjectRepository;
import com.codemangesystem.loginProcess.model_user.MyUser;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public static String read(String filePath) {
        FileReader fr = null;
        StringBuilder fileINFO = new StringBuilder();
        try {
            fr = new FileReader(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedReader br = new BufferedReader(fr);
        String tmp = null;

        try {
            while (((tmp = br.readLine()) != null)) {
                fileINFO.append(tmp);
                fileINFO.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return String.valueOf(fileINFO);
    }

    @Nested
    class analyzeAllCommitsTest {
        String repoPath = "/test";
        MyUser myUser = MyUser.builder()
                              .userId(1L)
                              .build();

        @Test
        void noDirectoryTest() throws GitAPIException, IOException {

        }

        @Test
        void noCommitTest() {

        }

        @Test
        void noJavaFileTest() {

        }

        @Test
        void successAnalyze() {

        }

        @Test
        void throwIOExceptionTest() {

        }

        @Test
        void throwGitAPIExceptionTest() {

        }
    }

    @Nested
    class analyzePartCommitsTest {
        @Test
        void noDirectoryTest() {

        }

        @Test
        void noProjectTest() {

        }

        @Test
        void noJavaFile() {

        }

        @Test
        void successAnalyze() {

        }

        @Test
        void throwIOExceptionTest() {

        }

        @Test
        void throwGitAPIExceptionTest() {

        }
    }

    @Nested
    class getCommitDiffTest {
        @Test
        void successGetDiffListTest() {

        }

        @Test
        void throwIOExceptionTest() {

        }

        @Test
        void throwGitAPIExceptionTest() {

        }
    }

    @Nested
    class setCommitDiffTest {
        @Test
        void successSetDiffTest() {

        }

        @Test
        void throwIOExceptionTest() {

        }
    }

    @Nested
    class getHeadSHA1Test {

    }

    @Nested
    class prepareTreeParserTest {

    }

    @Nested
    class takeCommitINFOTest {

    }

    @Nested
    class getFileContentTest {

    }

    @Nested
    class compareTwoContentTest {

    }

    @Nested
    class getMethodByContentTest {
        String content;

        @BeforeEach
        void setUp() {

        }

        @Test
        void sinTest() {
            // 初始資料
            content = read("src/test/resources/getMethodByContentData/content.txt");
            String[] excepted = {"sin(double)", "sin(double, int)", "factorial(double)", "main(String[])"};

            // 測試
            Map<String, String> actualResult = gitDiffAnalyzer.getMethodByContent(content);
            String[] actual = actualResult.keySet().toArray(new String[0]);
            assertArrayEquals(excepted, actual);
        }

        @Test
        void annotationTest() {
            // 初始資料
            content = read("src/test/resources/getMethodByContentData/havaAnnotationContent.txt");
            String[] excepted = {"register(MyUser)", "fetchRepository(String, String, String)",
                    "getProjectNames(String)", "deleteDataByProjectName(String, String)", "getFileDataByProjectName(String)"
                    , "pullByProjectName(String)", "login(LoginINFO)", "addSuperAccount()"};

            // 測試
            Map<String, String> actualResult = gitDiffAnalyzer.getMethodByContent(content);
            String[] actual = actualResult.keySet().toArray(new String[0]);
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
            oldMethod = read("src/test/resources/generateGitDiffData/oldMethod.txt");
            newMethod = read("src/test/resources/generateGitDiffData/newMethod.txt");
        }

        @Test
        @DisplayName("修改 hello world 比較測試")
        void hellWorldDiffTest() {
            // 測試
            String actual = GitDiffAnalyzer.generateGitDiff(oldMethod, newMethod);
            log.info("\n{}", actual);

            // 驗證
            String except = read("src/test/resources/generateGitDiffData/diffResult.txt");

            String[] actualLines = actual.split("\n");
            String[] exceptLines = except.split("\n");
            assertArrayEquals(actualLines, exceptLines);
        }

        @Test
        @DisplayName("一樣的 code 進行 diff")
        void noDiffTest() {
            newMethod = read("src/test/resources/generateGitDiffData/oldMethod.txt");

            // 測試
            String actual = GitDiffAnalyzer.generateGitDiff(oldMethod, newMethod);
            log.info("\n{}", actual);

            // 驗證
            String except = read("src/test/resources/generateGitDiffData/noDiffResult.txt");

            String[] actualLines = actual.split("\n");
            String[] exceptLines = except.split("\n");
            assertArrayEquals(actualLines, exceptLines);
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
            Files exceptFiles = null;
            for (Files files : project.getFiles()) {
                log.info("測試獲得的 files {}", files.getFileName());
                if (files.getFileName().equals(fileName)) {
                    exceptFiles = files;
                    break;
                }
            }
            assertNotNull(exceptFiles);

            List<Method> methodList = exceptFiles.getMethods();
            Method method = methodList.get(0);
            assertEquals(method.getMethodName(), "testMethod");

            List<DiffInfo> diffInfoList = method.getDiffInfoList();
            DiffInfo diffInfo = diffInfoList.get(0);
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
            DiffInfo diffInfo = diffInfoList.get(0);
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

            Method method = methodList.get(0);
            assertEquals(method.getMethodName(), "testMethod");

            List<DiffInfo> diffInfoList = method.getDiffInfoList();
            DiffInfo diffInfo = diffInfoList.get(0);
            assertEquals(diffInfo.getDiffCode(), "+ ssss");
        }
    }
}