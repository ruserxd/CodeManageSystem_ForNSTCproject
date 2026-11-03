package com.codemangesystem.git_process.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codemangesystem.git_process.model.git.RepositoryINFO;
import com.codemangesystem.git_process.repository.PersonalRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Slf4j
class GitFunctionTest {

  @Test
  @DisplayName("測試 GitFunction 不能實體化")
  void noConstructionTest() {
    UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
        GitFunction::new);
    assertEquals("This is a utility class and cannot be instantiated", exception.getMessage());
  }

  @Nested
  @DisplayName("測試 validateRepoUrl()")
  class validateRepoUrlTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "https://github.com/ruserxd/CodeManageSystem_ForNSTCproject.git",
        "https://github.com/spring-projects/spring-boot",
        "https://gitlab.com/owner/repo.name",
        "https://GitHub.com/owner/repo"
    })
    @DisplayName("合法的公開 repo URL 可以通過")
    void validUrlTest(String url) {
      assertDoesNotThrow(() -> GitFunction.validateRepoUrl(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "   ",
        "file:///etc/passwd",
        "/Users/someone/project",
        "../../backend",
        "http://github.com/owner/repo",
        "git@github.com:owner/repo.git",
        "ssh://git@github.com/owner/repo.git",
        "https://localhost/owner/repo",
        "https://10.0.0.1/owner/repo",
        "https://github.com.evil.com/owner/repo",
        "https://evil.com/owner/repo",
        "https://user:pass@github.com/owner/repo",
        "https://github.com:8443/owner/repo",
        "https://github.com/owner/repo?x=1",
        "https://github.com/owner/repo#readme",
        "https://github.com/owner",
        "https://github.com/owner/repo/tree/main",
        "https://github.com/owner/..",
        "https://github.com/../repo",
        "https://github.com/owner/.git",
        "https://github.com/owner/re%2F..%2Fpo"
    })
    @DisplayName("不合法或惡意的 URL 會被拒絕")
    void invalidUrlTest(String url) {
      assertThrows(IllegalArgumentException.class, () -> GitFunction.validateRepoUrl(url));
    }

    @Test
    @DisplayName("null 會被拒絕")
    void nullUrlTest() {
      assertThrows(IllegalArgumentException.class, () -> GitFunction.validateRepoUrl(null));
    }
  }

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
    void gitFileNotExist() {
      boolean actual = GitFunction.isInLocalGitDirectory("/test");
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