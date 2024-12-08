package com.codemangesystem.gitProcess.service;

import com.codemangesystem.gitProcess.model_Git.GitResult;
import com.codemangesystem.gitProcess.model_Git.GitStatus;
import com.codemangesystem.gitProcess.model_Repo.RepositoryINFO;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;


/*
 * 負責處理 git pull */
@Slf4j
@Service
public class GitPuller {
    private static final String DEFAULT_Remote = "origin";
    private static final String DEFAULT_BRANCH = "main";
    private final GetDataBse getDataBse;
    private final GitDiffAnalyzer gitDiffAnalyzer;

    public GitPuller(GetDataBse getDataBse, GitDiffAnalyzer gitDiffAnalyzer) {
        this.getDataBse = getDataBse;
        this.gitDiffAnalyzer = gitDiffAnalyzer;
    }

    /**
     * pull 更新本地端資料，並且更新本地端資料庫內容
     */
    public GitResult pullLocalRepository(RepositoryINFO repoINFO) {
        try (Git git = Git.open(new File(repoINFO.localPath))) {
            log.info("Try to pull {} at {}", repoINFO.repoName, repoINFO.localPath);
            PullResult pullResult = git.pull()
                                       .setRemote(DEFAULT_Remote)
                                       .setRemoteBranchName(DEFAULT_BRANCH)
                                       .call();

            // 如果 pull 的操作失敗
            if (!pullResult.isSuccessful()) {
                return GitResult.builder()
                                .message("發生不可預期 Failed pull " + repoINFO.repoName)
                                .status(GitStatus.PULL_FAILED)
                                .build();
            }

            // 獲取前一次存放的 Revstr
            String previousHeadRevstr = getDataBse.getHeadRevstr(repoINFO.repoName);
            return gitDiffAnalyzer.analyzePartCommits(repoINFO.localPath, previousHeadRevstr);
        } catch (IOException | GitAPIException error) {
            log.error("Pull 發生 {}", String.valueOf(error));
            return GitResult.builder()
                            .message("Pull 發生 " + error)
                            .status(GitStatus.PULL_FAILED)
                            .build();
        }
    }
}
