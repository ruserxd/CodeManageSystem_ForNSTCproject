package com.codemangesystem.gitProcess.service;

import com.codemangesystem.gitProcess.model_Git.GitResult;
import com.codemangesystem.gitProcess.model_Git.GitStatus;
import com.codemangesystem.gitProcess.model_Repo.RepoINFO;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public GitPuller(GetDataBse getDataBse, GitDiffAnalyzer gitDiffAnalyzer) {
        this.getDataBse = getDataBse;
        this.gitDiffAnalyzer = gitDiffAnalyzer;
    }

    /**
     * pull 更新本地端資料，並且更新本地端資料庫內容
     */
    public GitResult renewLocalRepository(RepoINFO repoINFO) {
        try (Git git = Git.open(new File(repoINFO.localPath))) {
            log.info("Try to pull {} at {}", repoINFO.repoName, repoINFO.localPath);
            PullResult pullResult = git.pull()
                                       .setRemote(DEFAULT_Remote)
                                       .setRemoteBranchName(DEFAULT_BRANCH)
                                       .call();
            if (!pullResult.isSuccessful()) {
                return GitResult.builder()
                                .message("發生不可預期 Failed pull " + repoINFO.repoName)
                                .status(GitStatus.PULL_FAILED)
                                .build();
            }
            String previousHeadRevstr = getDataBse.getHeadRevstr(repoINFO.repoName);
            gitDiffAnalyzer.analyzePartCommits(repoINFO.localPath, previousHeadRevstr);

            return GitResult.builder()
                            .message("Success pull " + repoINFO.repoName)
                            .status(GitStatus.PULL_SUCCESS)
                            .build();
        } catch (IOException | GitAPIException e) {
            log.error("Pull 發生 {}", String.valueOf(e));
            return GitResult.builder()
                            .message("Error when pull")
                            .status(GitStatus.PULL_FAILED)
                            .build();
        }
    }


}
