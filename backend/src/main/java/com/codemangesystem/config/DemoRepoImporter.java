package com.codemangesystem.config;

import com.codemangesystem.git_process.model.git.GitResult;
import com.codemangesystem.git_process.model.git.GitStatus;
import com.codemangesystem.git_process.service.GitCloner;
import com.codemangesystem.login_process.model.user.MyUser;
import com.codemangesystem.login_process.repository.MyUserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 啟動完成（HTTP 服務已可接受請求）後，依 app.demo.repos 把範例專案匯入 demo 帳號。
 * 任何失敗只記 log，絕不讓啟動失敗。
 */
@Component
@Slf4j
public class DemoRepoImporter {

  private static final String CLONE_COMMIT = "HEAD";
  // GitCloner.cloneRepository 在該帳戶已 clone 過時回傳的訊息開頭（GitCloner.java:79）
  private static final String ALREADY_CLONED_MESSAGE = "此帳戶已經有 clone 過";

  private final GitCloner gitCloner;
  private final MyUserRepository myUserRepository;
  private final String demoAccount;
  private final String demoRepos;
  private final AnalysisLimits analysisLimits;

  public DemoRepoImporter(GitCloner gitCloner, MyUserRepository myUserRepository,
      @Value("${app.demo.account:}") String demoAccount,
      @Value("${app.demo.repos:}") String demoRepos, AnalysisLimits analysisLimits) {
    this.gitCloner = gitCloner;
    this.myUserRepository = myUserRepository;
    this.demoAccount = demoAccount;
    this.demoRepos = demoRepos;
    this.analysisLimits = analysisLimits;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void importDemoRepos() {
    try {
      List<String> urls = parseUrls(demoRepos);
      if (urls.isEmpty()) {
        log.info("app.demo.repos 未設定，略過匯入 demo 範例專案");
        return;
      }
      if (demoAccount == null || demoAccount.isBlank()) {
        log.warn("app.demo.account 未設定，略過匯入 demo 範例專案");
        return;
      }
      Optional<MyUser> demoUser = myUserRepository.findByUserAccount(demoAccount);
      if (demoUser.isEmpty()) {
        log.warn("找不到 demo 帳號，略過匯入 demo 範例專案");
        return;
      }
      Long demoUserId = demoUser.get().getUserId();
      for (String url : urls) {
        importOne(url, demoUserId);
      }
    } catch (Exception e) {
      log.error("匯入 demo 範例專案時發生未預期的錯誤", e);
    }
  }

  private void importOne(String url, Long demoUserId) {
    try {
      GitResult result = gitCloner.cloneRepository(url, CLONE_COMMIT, demoUserId,
          analysisLimits.maxCommitsFor("DEMO"));
      GitStatus status = result.getStatus();
      if (status == GitStatus.CLONE_SUCCESS || status == GitStatus.PULL_SUCCESS) {
        log.info("demo 範例專案匯入成功 {} ({})", url, status);
      } else if (result.getMessage() != null
          && result.getMessage().contains(ALREADY_CLONED_MESSAGE)) {
        log.info("demo 範例專案已存在 {} : {}", url, result.getMessage());
      } else {
        log.warn("demo 範例專案匯入失敗 {} ({}) : {}", url, status, result.getMessage());
      }
    } catch (Exception e) {
      log.error("demo 範例專案匯入失敗 {}", url, e);
    }
  }

  private static List<String> parseUrls(String repos) {
    if (repos == null) {
      return List.of();
    }
    return Arrays.stream(repos.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }
}
