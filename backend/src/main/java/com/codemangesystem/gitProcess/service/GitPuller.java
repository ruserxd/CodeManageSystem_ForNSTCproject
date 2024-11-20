package com.codemangesystem.gitProcess.service;

import com.codemangesystem.gitProcess.model_Data.Files;
import com.codemangesystem.gitProcess.model_Git.CloneStatus;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;


/*
 * 負責處理 git pull */
@Slf4j
@Service
public class GitPuller {
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
    public CloneStatus renewLocalRepository(String repoPath) {
        try (Git git = Git.open(new File(repoPath))) {
            log.info("Try to pull {} ...", repoPath);
            PullResult result = git.pull()
                    .setRemote("origin")
                    .setRemoteBranchName(DEFAULT_BRANCH)
                    .call();

            String projectName = GitFunction.getRepoNameFromUrl(repoPath);
            String previousHeadRevstr = getDataBse.getHeadRevstr(projectName);

            if (result.isSuccessful()) {
                log.info("Pull successful");
                log.info("更新資料庫 {} 的內容", projectName);
                List<Files> analyzedFiles = gitDiffAnalyzer.analyzePartCommits(repoPath, previousHeadRevstr);

                // TODO: 不一定是失敗，可能出現不是 Java 檔案的更新
                if (analyzedFiles == null) {
                    log.warn("No files were analyzed in the repository");
                    return CloneStatus.PULL_FAILED;
                }

                return CloneStatus.PULL_SUCCESS;
            } else {
                log.info("Pull failed");
                return CloneStatus.PULL_FAILED;
            }
        } catch (IOException | GitAPIException error) {
            log.error("Pull 更新資料庫出現 {}", error.getMessage());
        } finally {
            log.info("renewRepositoryLocally 結束");
        }

        return CloneStatus.PULL_FAILED;
    }
}
