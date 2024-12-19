package com.codemangesystem.gitProcess.service;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * Git 部分經常使用的 static 方法
 */
@Slf4j
public class GitFunction {
    /**
     * 從儲存庫 URL 中取得專案名稱
     */
    public static String getRepoNameFromUrl(String repoUrl) {
        // 將網址透過 "/" 分開並存在陣列內
        String[] parts = repoUrl.split("/");

        // 抓取最後面的專案名稱
        String repoName = parts[parts.length - 1];

        // .git 的部分換成 ""
        return repoName.replace(".git", "");
    }

    /**
     * 檢查指定路徑是否為已存在 Git 儲存庫
     */
    public static boolean isLocalCloned(String localPath) {
        File gitDir = new File(localPath, ".git");
        return gitDir.exists();
    }
}
