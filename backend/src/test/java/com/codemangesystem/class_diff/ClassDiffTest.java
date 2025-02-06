package com.codemangesystem.class_diff;

import com.codemangesystem.git_process.repository.PersonalRepository;
import com.codemangesystem.git_process.repository.ProjectRepository;
import com.codemangesystem.git_process.service.GitDiffAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.codemangesystem.FileReader.getFileCode;
import static org.mockito.Mockito.mock;

/*
 * 負責驗證做出來的 git-diff 是否符合實際需求
 * 以及展現本專案的功能
 * */
@Slf4j
public class ClassDiffTest {
    String newContent;
    String oldContent;
    GitDiffAnalyzer gitDiffAnalyzer;
    ProjectRepository projectRepository;
    PersonalRepository personalRepository;

    // 臨時的 path, 供測試使用
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        newContent = "";
        oldContent = "";
        projectRepository = mock(ProjectRepository.class);
        personalRepository = mock(PersonalRepository.class);
        gitDiffAnalyzer = new GitDiffAnalyzer(projectRepository, personalRepository);
    }

    // 模擬 git 的操作
    void simulateGitDiff(String oldContent, String newContent) throws IOException, GitAPIException {
        // 創建 git repository
        Path repoPath = tempDir.resolve("test-gitDiff");
        java.nio.file.Files.createDirectory(repoPath);
        Git git = Git.init().setDirectory(repoPath.toFile()).call();

        // 創建 java 檔案
        Path javaFile = repoPath.resolve("test.java");

        // 第一次 commit
        java.nio.file.Files.write(javaFile, oldContent.getBytes());
        git.add().addFilepattern("test.java").call();
        RevCommit oldCommit = git.commit().setMessage("initial test.java").call();

        // 第二次 commit
        java.nio.file.Files.write(javaFile, newContent.getBytes());
        git.add().addFilepattern("test.java").call();
        RevCommit newCommit = git.commit().setMessage("update test.java").call();

        Repository repository = git.getRepository();
        List<DiffEntry> diffEntries = git.diff()
                                         .setOldTree(gitDiffAnalyzer.prepareTreeParser(repository, oldCommit))
                                         .setNewTree(gitDiffAnalyzer.prepareTreeParser(repository, newCommit))
                                         .call();

        // 輸出 git diff
        outGitDiff(repository, diffEntries);
    }

    /**
     * 輸出 git diff 資訊
     */
    void outGitDiff(Repository repository, List<DiffEntry> diffEntries) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (DiffFormatter formatter = new DiffFormatter(outputStream)) {
            formatter.setRepository(repository);

            StringBuilder diffContent = new StringBuilder();
            for (DiffEntry diff : diffEntries) {
                // 獲取檔案差異信息
                diffContent.append("Changed file: ").append(diff.getNewPath()).append("\n");
                diffContent.append("Change type: ").append(diff.getChangeType()).append("\n");

                // 獲取具體的修改內容
                formatter.format(diff);
                diffContent.append(outputStream);
                outputStream.reset();

                diffContent.append("\n");
            }

            log.info("{}", diffContent);
        }
    }

    @Test
    @DisplayName("測試 normal_function 的部分")
    void test_normal_function() throws GitAPIException, IOException {
        newContent = getFileCode("src/test/resources/classData/normal_function/newClass.java");
        oldContent = getFileCode("src/test/resources/classData/normal_function/oldClass.java");
        List<Pair<String, String>> actual = gitDiffAnalyzer.compareTwoContent(oldContent, newContent);

        // 本系統的方法 diff
        log.info("總共有 {} 個方法", actual.size());
        for (Pair<String, String> p : actual) {
            log.info("\n{} {}", p.getLeft(), p.getRight());
        }

        // git 原本的 diff
        simulateGitDiff(oldContent, newContent);
    }
}
