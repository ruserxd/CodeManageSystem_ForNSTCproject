package com.codemangesystem.gitProcess.service;

import java.io.File;

/**
 * Git 部分經常使用的 static 方法
 */
public class GitFunction {
    /**
     * 從儲存庫 URL 中取得專案名稱
     */
    public static String getRepoNameFromUrl(String repoUrl) {
        // 將網址透過 "/" 分開並存在陣列內
        String[] parts = repoUrl.split("/");

        // 抓取最後面的專案名稱
        String repoNameWithExtension = parts[parts.length - 1];

        // .git 的部分換成 ""
        return repoNameWithExtension.replace(".git", "");
    }

    /**
     * 檢查指定路徑是否為已存在 Git 儲存庫
     */
    public static boolean isRepositoryClonedLocally(String path) {
        File gitDir = new File(path, ".git");
        return gitDir.exists();
    }
}
