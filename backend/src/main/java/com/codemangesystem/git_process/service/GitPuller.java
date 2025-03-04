package com.codemangesystem.git_process.service;

import com.codemangesystem.git_process.model.git.GitResult;
import com.codemangesystem.git_process.model.git.GitStatus;
import com.codemangesystem.git_process.model.git.RepositoryINFO;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;


/**
 * 負責處理 git pull
 */
@Slf4j
@Service
public class GitPuller {
    private static final String DEFAULT_REMOTE = "origin";
    private static final String DEFAULT_BRANCH = "main";
    private final DataBaseService dataBaseService;
    private final GitDiffAnalyzer gitDiffAnalyzer;

    public GitPuller(DataBaseService dataBaseService, GitDiffAnalyzer gitDiffAnalyzer) {
        this.dataBaseService = dataBaseService;
        this.gitDiffAnalyzer = gitDiffAnalyzer;
    }

    /**
     * pull 更新本地端資料，並且更新本地端資料庫內容
     */
    public GitResult pullLocalRepository(RepositoryINFO repoINFO) {
        try {
            Git git = Git.open(new File(repoINFO.localPath));
            log.info("Try to pull {} at {}", repoINFO.repoName, repoINFO.localPath);
            PullResult pullResult = git.pull()
                                       .setRemote(DEFAULT_REMOTE)
                                       .setRemoteBranchName(DEFAULT_BRANCH)
                                       .call();

            // 如果 pull 的操作失敗
            if (!pullResult.isSuccessful()) {
                return GitResult.builder()
                                .message("發生不可預期 Failed pull " + repoINFO.repoName)
                                .status(GitStatus.PULL_FAILED)
                                .build();
            }

            // 手動關閉 git
            git.close();

            // 獲取前一次存放的 Revstr
            String previousHeadRevstr = dataBaseService.getHeadRevstr(repoINFO.repoName);
            return gitDiffAnalyzer.analyzePartCommits(repoINFO.localPath, previousHeadRevstr);
        } catch (IOException error) {
            log.error("Pull 發生 {}", error.getMessage());
            return GitResult.builder()
                            .message("Pull 發生 " + error.getMessage())
                            .status(GitStatus.PULL_FAILED)
                            .build();
        } catch (GitAPIException error) {
            log.error("發生 {}", error.getMessage());
            return GitResult.builder()
                            .message("Pull 發生 " + error.getMessage())
                            .status(GitStatus.PULL_FAILED)
                            .build();
        }
    }
}
