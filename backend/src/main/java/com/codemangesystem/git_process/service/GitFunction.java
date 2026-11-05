package com.codemangesystem.git_process.service;

import com.codemangesystem.git_process.model.git.RepositoryINFO;
import com.codemangesystem.git_process.repository.PersonalRepository;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Git 部分經常使用的 static 方法
 */
@Slf4j
public class GitFunction {

  /*
   * 防止被實體化*/
  public GitFunction() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * 允許 clone 的 Git 託管平台。限制 host 可以避免 SSRF（連到內網）與讀取伺服器本機的 repo（file://、本機路徑）
   */
  private static final Set<String> ALLOWED_HOSTS = Set.of("github.com", "gitlab.com", "bitbucket.org");

  /**
   * 路徑必須剛好是 /owner/repo（可帶 .git、可帶一個結尾 /），名稱只允許英數與 . _ -
   */
  private static final Pattern REPO_PATH = Pattern.compile("^/[A-Za-z0-9._-]+/[A-Za-z0-9._-]+/?$");

  /**
   * clone 前驗證使用者提供的 repo URL，不合法時丟出 IllegalArgumentException（訊息可直接回給使用者）
   */
  public static void validateRepoUrl(String repoUrl) {
    if (repoUrl == null || repoUrl.isBlank()) {
      throw new IllegalArgumentException("repo URL 不可為空");
    }

    URI uri;
    try {
      uri = new URI(repoUrl);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("repo URL 格式錯誤");
    }

    if (!"https".equalsIgnoreCase(uri.getScheme())) {
      throw new IllegalArgumentException("只接受 https 開頭的 repo URL");
    }
    if (uri.getRawUserInfo() != null || uri.getPort() != -1
        || uri.getRawQuery() != null || uri.getRawFragment() != null) {
      throw new IllegalArgumentException("repo URL 不可包含帳密、port、query 或 fragment");
    }
    if (uri.getHost() == null || !ALLOWED_HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException("只接受 " + String.join("、", ALLOWED_HOSTS) + " 上的 repo");
    }

    String path = uri.getRawPath();
    if (path == null || !REPO_PATH.matcher(path).matches()) {
      throw new IllegalArgumentException("repo URL 必須是 https://<host>/<owner>/<repo> 的格式");
    }
    // 名稱會被用來組 clone 路徑，"."、".." 或只剩 ".git" 都會讓路徑跳出 src/cloneCode/
    for (String segment : path.substring(1).split("/")) {
      if (segment.equals(".") || segment.equals("..")) {
        throw new IllegalArgumentException("repo URL 不可包含 . 或 ..");
      }
    }
    String repoName = getRepoNameFromUrl(repoUrl);
    if (repoName.isBlank() || repoName.equals(".") || repoName.equals("..")) {
      throw new IllegalArgumentException("無法從 repo URL 取得專案名稱");
    }
  }

  /**
   * 將 repo URL 正規化成 https://&lt;host&gt;/&lt;owner&gt;/&lt;repo&gt;（ADR 0001）。
   * 先通過 validateRepoUrl，再把 host 與 path 轉小寫、去掉一個結尾的 /、再去掉一個結尾的 .git。
   * 只去一個，不保證冪等。不合法時丟出 IllegalArgumentException
   */
  public static String normalizeRepoUrl(String repoUrl) {
    validateRepoUrl(repoUrl);

    URI uri = URI.create(repoUrl);
    String host = uri.getHost().toLowerCase(Locale.ROOT);
    String path = uri.getRawPath().toLowerCase(Locale.ROOT);

    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    if (path.endsWith(".git")) {
      path = path.substring(0, path.length() - ".git".length());
    }
    // 驗證器的空名稱檢查區分大小寫，轉小寫去掉 .git 後名稱可能是空的、"." 或 ".."
    String repoName = path.substring(path.lastIndexOf('/') + 1);
    if (repoName.isEmpty() || repoName.equals(".") || repoName.equals("..")) {
      throw new IllegalArgumentException("無法從 repo URL 取得專案名稱");
    }
    return "https://" + host + path;
  }

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
  public static boolean isInLocalGitDirectory(String localPath) {
    File gitDir = new File(localPath, ".git");
    return gitDir.exists();
  }

  /**
   * 判斷是否使用者已經 clone 過
   */
  public static boolean isUserCloned(Long userId, RepositoryINFO repoINFO,
      PersonalRepository personalRepository) {
    List<String> projectNames = personalRepository.findProjectNameByUserId(userId);
    for (String projectName : projectNames) {
      if (projectName.equals(repoINFO.repoName)) {
        log.info("Repository already exists at: {}", repoINFO.localPath);
        return true;
      }
    }
    return false;
  }
}
