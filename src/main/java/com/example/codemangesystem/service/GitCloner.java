package com.example.codemangesystem.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

// 處理有關 Git clone 的操作
@Service
public class GitCloner {
    private static final Logger logger = LoggerFactory.getLogger(GitCloner.class);
    private static final String DEFAULT_BRANCH = "main";
    private static final String CLONE_LOCAL_BASE_PATH = "src/cloneCode/";

    // 判斷儲存庫是否需要 clone 到本地資料夾，並回傳最終儲存庫存放的路徑
    public String cloneRepository(String repoUrl) throws GitAPIException, IOException {
        String repoName = getRepoNameFromUrl(repoUrl);
        String localPath = CLONE_LOCAL_BASE_PATH + repoName;
        try {
            // 如果本地資料夾已經存在， pull 更新本地端資料並且直接回傳路徑
            if (isRepositoryClonedLocally(localPath)) {
                logger.info("Repository already exists at: {}", localPath);
                renewRepositoryLocally(localPath);
                return localPath;
            }
            logger.info("Cloning to {} ....", repoUrl);
            // 將資料 clone 下來， Git 物件命名為 ignored ，因為在這個特定的 try 區塊中，實際上並不需要直接使用這個物件
            try (Git ignored = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(localPath))
                    .call()) {

                logger.info("Repository success cloned to: {}", localPath);
                return localPath;
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
