package com.example.codemangesystem.GitProcess.service;

import com.example.codemangesystem.GitProcess.model_Git.CloneResult;
import com.example.codemangesystem.GitProcess.model_Git.CloneStatus;
import com.example.codemangesystem.GitProcess.model_Data.Files;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

// 處理有關 Git clone 的操作
@Service
public class GitCloner {
    private static final Logger logger = LoggerFactory.getLogger(GitCloner.class);
    private static final String DEFAULT_BRANCH = "main";
    private static final String CLONE_LOCAL_BASE_PATH = "src/cloneCode/";

    private final GitDiffAnalyzer gitDiffAnalyzer;

    @Autowired
    public GitCloner(GitDiffAnalyzer gitDiffAnalyzer) {
        this.gitDiffAnalyzer = gitDiffAnalyzer;
    }

    // 判斷儲存庫是否需要 clone 到本地資料夾，並回傳最終儲存庫存放的路徑
    public CloneResult cloneRepository(String repoUrl) throws GitAPIException, IOException {
        String repoName = getRepoNameFromUrl(repoUrl);
        String localPath = CLONE_LOCAL_BASE_PATH + repoName;

        try {

            // 如果本地資料夾已經存在， pull 更新本地端資料並且直接回傳路徑
            if (isRepositoryClonedLocally(localPath)) {
                logger.info("Repository already exists at: {}", localPath);
                try {
                    renewRepositoryLocally(localPath);
                    // TODO: 更新資料庫的內容
                    logger.info("Successfully pulled and updated repository at {}", localPath);
                    return CloneResult.builder()
                            .status(CloneStatus.PULL_SUCCESS)
                            .path(localPath)
                            .build();
                } catch (Exception e) {
                    // 如果更新失敗，記錄錯誤並決定如何處理
                    logger.error("Failed to update existing repository at {}", localPath, e);
                    return CloneResult.builder().status(CloneStatus.PULL_FAILED).build();
                }
            }

            logger.info("Cloning to {} ....", repoUrl);

            // 將資料 clone 下來， Git 物件命名為 ignored ，因為在這個特定的 try 區塊中，實際上並不需要直接使用這個物件
            // 只是要透過 Git 物件將資料 clone 下來
            // clone 成功接續將資料分類存入資料庫內
            try (Git ignored = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(localPath))
                    .call()) {

                logger.info("成功 clone: {}", localPath);
                logger.info("嘗試分類 -> gitDiffAnalyzer");
                List<Files> analyzedFiles = gitDiffAnalyzer.analyzeCommits(localPath);

                if (analyzedFiles == null || analyzedFiles.isEmpty()) {
                    logger.warn("No files were analyzed in the repository: {}", localPath);
                    return CloneResult.builder().status(CloneStatus.ANALYSIS_FAILED).build();
                }

                logger.info("成功將資料分類完成");
                return CloneResult.builder().status(CloneStatus.CLONE_SUCCESS).path(localPath).build();
            }
        } catch (GitAPIException e) {
            logger.error("Failed clone to {}", repoUrl, e);
            throw e;
        }
    }

    // 從儲存庫 URL 中取得專案名稱
    private String getRepoNameFromUrl(String repoUrl) {
        // 將網址透過 "/" 分開並存在陣列內
        String[] parts = repoUrl.split("/");
        // 抓取最後面的專案名稱
        String repoNameWithExtension = parts[parts.length - 1];
        // .git 的部分換成 ""
        return repoNameWithExtension.replace(".git", "");
    }

    // 檢查指定路徑是否為已存在 Git 儲存庫
    private boolean isRepositoryClonedLocally(String path) {
        File gitDir = new File(path, ".git");
        return gitDir.exists();
    }

    // pull 更新本地端資料
    private void renewRepositoryLocally(String repoPath) {
        try (Git git = Git.open(new File(repoPath))) {
            logger.info("Try to pull {} ...", repoPath);
            PullResult result = git.pull()
                    .setRemote("origin")
                    .setRemoteBranchName(DEFAULT_BRANCH)
                    .call();

            if (result.isSuccessful()) {
                logger.info("Pull successful");
            } else {
                logger.info("Pull failed");
            }
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }
}
