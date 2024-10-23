package com.example.codemangesystem.GitProcess.service;

import com.example.codemangesystem.GitProcess.model_Git.CloneResult;
import com.example.codemangesystem.GitProcess.model_Git.CloneStatus;
import com.example.codemangesystem.GitProcess.model_Data.Files;
import com.example.codemangesystem.GitProcess.repository.ProjectRepository;
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

/**
 * 處理有關 Git clone, pull的操作
 * */
@Service
public class GitCloner {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitCloner.class);
    private static final String DEFAULT_BRANCH = "main";
    private static final String CLONE_LOCAL_BASE_PATH = "src/cloneCode/";

    private final GitDiffAnalyzer gitDiffAnalyzer;
    private final ProjectRepository projectRepository;
    @Autowired
    public GitCloner(GitDiffAnalyzer gitDiffAnalyzer, ProjectRepository projectRepository) {
        this.gitDiffAnalyzer = gitDiffAnalyzer;
        this.projectRepository = projectRepository;
    }

    // 判斷儲存庫是否需要 clone 到本地資料夾，並回傳最終儲存庫存放的路徑
    public CloneResult cloneRepository(String repoUrl) throws GitAPIException, IOException {
        String repoName = getRepoNameFromUrl(repoUrl);
        String localPath = CLONE_LOCAL_BASE_PATH + repoName;

        try {

            // 如果本地資料夾已經存在， pull 更新本地端資料並且直接回傳路徑
            if (isRepositoryClonedLocally(localPath)) {
                LOGGER.info("Repository already exists at: {}", localPath);
                try {
                    CloneStatus cloneStatus =  renewRepositoryLocally(localPath);
                    LOGGER.info("Successfully pulled and updated repository at {}", localPath);

                    return CloneResult.builder()
                            .status(cloneStatus)
                            .path(localPath)
                            .build();
                } catch (Exception e) {
                    // 如果更新失敗，記錄錯誤並決定如何處理
                    LOGGER.error("Failed to update existing repository at {}", localPath, e);
                    return CloneResult.builder()
                            .status(CloneStatus.PULL_FAILED)
                            .build();
                }
            }

            LOGGER.info("Cloning to {} ....", repoUrl);

            /* 將資料 clone 下來， Git 物件命名為 ignored ，因為在這個特定的 try 區塊中，實際上並不需要直接使用這個物件
             * 只是要透過 Git 物件將資料 clone 下來
             * lone 成功接續將資料分類存入資料庫內 */
            try (Git ignored = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(localPath))
                    .call()) {

                LOGGER.info("成功 clone: {}", localPath);
                LOGGER.info("嘗試分類 -> gitDiffAnalyzer");
                List<Files> analyzedFiles = gitDiffAnalyzer.analyzeAllCommits(localPath);

                if (analyzedFiles == null || analyzedFiles.isEmpty()) {
                    LOGGER.warn("No files were analyzed in the repository: {}", localPath);
                    return CloneResult.builder()
                            .status(CloneStatus.ANALYSIS_FAILED)
                            .build();
                }

                LOGGER.info("成功將資料分類完成");
                return CloneResult.builder()
                        .status(CloneStatus.CLONE_SUCCESS)
                        .path(localPath)
                        .build();
            }
        } catch (GitAPIException e) {
            LOGGER.error("Failed clone to {}", repoUrl, e);
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
    private CloneStatus renewRepositoryLocally(String repoPath) {
        try (Git git = Git.open(new File(repoPath))) {
            LOGGER.info("Try to pull {} ...", repoPath);
            PullResult result = git.pull()
                    .setRemote("origin")
                    .setRemoteBranchName(DEFAULT_BRANCH)
                    .call();
            String projectName = getRepoNameFromUrl(repoPath);

            String previousHeadRevstr = projectRepository.findHeadRevstrByProjectName(projectName);

            if (result.isSuccessful()) {
                LOGGER.info("Pull successful");
                LOGGER.info("更新資料庫 {} 的內容", projectName);
                List<Files> analyzedFiles = gitDiffAnalyzer.analyzePartCommits(repoPath, previousHeadRevstr);

                if (analyzedFiles == null) {
                    LOGGER.warn("No files were analyzed in the repository");
                    return CloneStatus.PULL_FAILED;
                }

                return CloneStatus.PULL_SUCCESS;
            } else {
                LOGGER.info("Pull failed");
                return CloneStatus.PULL_FAILED;
            }
        } catch (IOException | GitAPIException e) {
            LOGGER.error("Pull 更新資料庫出現 " + e);
        } finally {
            LOGGER.info("renewRepositoryLocally 結束");
        }

        return CloneStatus.PULL_FAILED;
    }
}
