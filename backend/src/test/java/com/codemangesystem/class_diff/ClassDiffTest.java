package com.codemangesystem.class_diff;

import com.codemangesystem.git_process.repository.PersonalRepository;
import com.codemangesystem.git_process.repository.ProjectRepository;
import com.codemangesystem.git_process.service.GitDiffAnalyzer;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.codemangesystem.FileReader.read;
import static org.mockito.Mockito.mock;

/*
 * 負責驗證做出來的 git-diff 是否符合實際需求
 * 以及展現本專案的功能
 * */
public class ClassDiffTest {
    String newContent;
    String oldContent;
    GitDiffAnalyzer gitDiffAnalyzer;
    ProjectRepository projectRepository;
    PersonalRepository personalRepository;

    @BeforeEach
    void setUp() {
        newContent = "";
        oldContent = "";
        projectRepository = mock(ProjectRepository.class);
        personalRepository = mock(PersonalRepository.class);
        gitDiffAnalyzer = new GitDiffAnalyzer(projectRepository, personalRepository);
    }

    @Test
    @DisplayName("測試 normal_function 的部分")
    void test_normal_function() {
        newContent = read("src/test/resources/classData/normal_function/newClass.java");
        oldContent = read("src/test/resources/classData/normal_function/oldClass.java");
        List<Pair<String, String>> actual = gitDiffAnalyzer.compareTwoContent(newContent, oldContent);

        for (Pair<String, String> p : actual) {
            System.out.println(p.getLeft() + ' ' + p.getRight());
        }
    }
}
