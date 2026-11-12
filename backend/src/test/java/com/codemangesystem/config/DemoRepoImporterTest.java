package com.codemangesystem.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codemangesystem.git_process.model.git.GitResult;
import com.codemangesystem.git_process.model.git.GitStatus;
import com.codemangesystem.git_process.service.GitCloner;
import com.codemangesystem.login_process.model.user.MyUser;
import com.codemangesystem.login_process.repository.MyUserRepository;
import com.codemangesystem.quota.AnalysisLimits;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DemoRepoImporterTest {

  private static final String ACCOUNT = "demo";
  private static final Long DEMO_ID = 42L;
  private static final int USER_MAX = 3;
  private static final String URL_A = "https://github.com/a/repoA.git";
  private static final String URL_B = "https://github.com/b/repoB.git";

  private GitCloner gitCloner;
  private MyUserRepository myUserRepository;

  @BeforeEach
  void setUp() {
    gitCloner = mock(GitCloner.class);
    myUserRepository = mock(MyUserRepository.class);
  }

  private DemoRepoImporter importer(String account, String repos) {
    return new DemoRepoImporter(gitCloner, myUserRepository, account, repos,
        new AnalysisLimits(USER_MAX, 100, 3, 0));
  }

  private void demoUserExists() {
    MyUser user = MyUser.builder().userId(DEMO_ID).userAccount(ACCOUNT).build();
    when(myUserRepository.findByUserAccount(ACCOUNT)).thenReturn(Optional.of(user));
  }

  @Test
  @DisplayName("未設定、空白或只有逗號時不呼叫 GitCloner")
  void blankReposDoNothing() {
    for (String repos : new String[]{"", "   ", ",,", " , "}) {
      assertDoesNotThrow(() -> importer(ACCOUNT, repos).importDemoRepos());
    }
    verifyNoInteractions(gitCloner);
  }

  @Test
  @DisplayName("demo 帳號不存在或 account 為空時不呼叫 GitCloner")
  void missingDemoAccountDoesNothing() {
    when(myUserRepository.findByUserAccount(ACCOUNT)).thenReturn(Optional.empty());
    assertDoesNotThrow(() -> importer(ACCOUNT, URL_A).importDemoRepos());
    assertDoesNotThrow(() -> importer("", URL_A).importDemoRepos());
    verifyNoInteractions(gitCloner);
  }

  @Test
  @DisplayName("兩個 URL（含空白）各以 demo 的 userId 呼叫一次")
  void importsEachUrlWithDemoUserId() throws Exception {
    demoUserExists();
    when(gitCloner.cloneRepository(anyString(), anyString(), anyLong(), anyInt()))
        .thenReturn(GitResult.builder().status(GitStatus.CLONE_SUCCESS).message("ok").build());

    importer(ACCOUNT, "  " + URL_A + " , " + URL_B + "  ").importDemoRepos();

    verify(gitCloner).cloneRepository(URL_A, "HEAD", DEMO_ID, USER_MAX);
    verify(gitCloner).cloneRepository(URL_B, "HEAD", DEMO_ID, USER_MAX);
  }

  @Test
  @DisplayName("其中一個丟例外或回傳失敗時，另一個仍被處理")
  void failureDoesNotStopOthers() throws Exception {
    demoUserExists();
    when(gitCloner.cloneRepository(URL_A, "HEAD", DEMO_ID, USER_MAX))
        .thenThrow(new IllegalArgumentException("bad url"));
    when(gitCloner.cloneRepository(URL_B, "HEAD", DEMO_ID, USER_MAX))
        .thenReturn(GitResult.builder().status(GitStatus.CLONE_FAILED)
            .message("此帳戶已經有 clone 過 " + URL_B).build());

    assertDoesNotThrow(() -> importer(ACCOUNT, URL_A + "," + URL_B).importDemoRepos());

    verify(gitCloner).cloneRepository(URL_A, "HEAD", DEMO_ID, USER_MAX);
    verify(gitCloner).cloneRepository(URL_B, "HEAD", DEMO_ID, USER_MAX);
  }

  @Test
  @DisplayName("findByUserAccount 丟例外時不對外丟出")
  void repositoryExceptionIsSwallowed() {
    when(myUserRepository.findByUserAccount(ACCOUNT)).thenThrow(new RuntimeException("db down"));
    assertDoesNotThrow(() -> importer(ACCOUNT, URL_A).importDemoRepos());
    verifyNoInteractions(gitCloner);
  }
}
