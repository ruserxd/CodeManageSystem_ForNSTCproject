package com.codemangesystem.gitProcess.service;

import com.codemangesystem.gitProcess.model_Data.Files;
import com.codemangesystem.gitProcess.model_Git.GitResult;
import com.codemangesystem.gitProcess.model_Git.GitStatus;
import com.codemangesystem.gitProcess.model_Repo.RepoINFO;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * 處理有關 Git clone 的操作
 */
// TODO: 遇上分支上的問題
@Slf4j
@Service
public class GitCloner {
    private static final String CLONE_LOCAL_BASE_PATH = "src/cloneCode/";

    private final GitDiffAnalyzer gitDiffAnalyzer;
    private final GitPuller gitPuller;

    @Autowired
    public GitCloner(GitDiffAnalyzer gitDiffAnalyzer, GitPuller gitPuller) {
        this.gitDiffAnalyzer = gitDiffAnalyzer;
        this.gitPuller = gitPuller;
    }

    // TODO: 使用者 GitHub 的權限

    /**
     * 判斷儲存庫是否需要 clone 到本地資料夾，並回傳最終儲存庫存放的路徑
     */
    public GitResult cloneRepository(String repoUrl, String commitId) throws GitAPIException, IOException {
        RepoINFO repoINFO = RepoINFO.builder()
                .repoName(GitFunction.getRepoNameFromUrl(repoUrl))
                .localPath(CLONE_LOCAL_BASE_PATH + GitFunction.getRepoNameFromUrl(repoUrl))
                .build();

        log.info("當前 repoINFO path : {}  name : {}", repoINFO.localPath, repoINFO.repoName);
        try {
            // 如果本地資料夾已經存在， pull 更新本地端資料並且直接回傳 GitResult
            if (GitFunction.isCloned(repoINFO.localPath)) {
                log.info("Repository already exists at: {}", repoINFO.localPath);
                return gitPuller.renewLocalRepository(repoINFO);
            }

            //TODO: Refactor 到這邊
            log.info("Cloning to {}", repoUrl);

            CloneCommand cloneCommand = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(repoINFO.localPath));

            // 將資料 clone 下來，try 達到 close
            // 只是要透過 Git 物件將資料 clone 下來
            // clone 成功接續將資料分類存入資料庫內
            try (Git git = cloneCommand.call();) {
                if (!Objects.equals(commitId, "HEAD")) {
                    log.info("commitId 為 {}", commitId);
                    try {
                        ObjectId specifyCommit = git.getRepository()
                                .resolve(commitId);
                        if (specifyCommit == null) {
                            log.error("Commit {} not found in repository", commitId);
                            return GitResult.builder()
                                    .status(GitStatus.CLONE_FAILED)
                                    .message("指定的 Commit ID 不存在: " + commitId)
                                    .build();
                        }

                        git.checkout()
                                .setName(specifyCommit.getName())
                                .call();
                        log.info("成功 checked out commit: {}", commitId);
                    } catch (RevisionSyntaxException | IOException | GitAPIException e) {
                        throw new RuntimeException(e);
                    }
                }

                log.info("成功 clone: {}", repoINFO.localPath);
                log.info("嘗試分類 -> gitDiffAnalyzer");
                List<Files> analyzedFiles = gitDiffAnalyzer.analyzeAllCommits(repoINFO.localPath);

                if (analyzedFiles == null || analyzedFiles.isEmpty()) {
                    log.warn("No files were analyzed in the repository: {}", repoINFO.localPath);
                    return GitResult.builder()
                            .status(GitStatus.ANALYSIS_FAILED)
                            .message("No files were analyzed in the repository: " + repoINFO.localPath)
                            .build();
                }

                log.info("成功將資料分類完成");
                return GitResult.builder()
                        .status(GitStatus.CLONE_SUCCESS)
                        .message("成功將資料分類完成")
                        .build();
            }
        } catch (GitAPIException e) {
            log.error("Failed clone to {}", repoUrl, e);
            return GitResult.builder()
                    .status(GitStatus.CLONE_FAILED)
                    .message("Failed to clone " + e)
                    .build();
        }
    }
}
