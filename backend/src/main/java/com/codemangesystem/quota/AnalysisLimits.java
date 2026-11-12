package com.codemangesystem.quota;

import java.io.File;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 單次分析的上限：依角色的 commit 數上限、每人專案數上限、全站同時只跑一個分析的槽位、clone 前的磁碟空間檢查。
 *
 * <p>角色只認 ADMIN；其他值（USER、DEMO、缺少 claim 的 null）一律視為一般使用者。
 */
@Component
public class AnalysisLimits {

  private static final String ADMIN_ROLE = "ADMIN";
  private static final long BYTES_PER_MB = 1024L * 1024L;

  /** clone 前可用磁碟空間低於門檻。 */
  public static class InsufficientDiskSpaceException extends RuntimeException {
    public InsufficientDiskSpaceException(String message) {
      super(message);
    }
  }

  private final int userMaxCommits;
  private final int adminMaxCommits;
  private final int maxProjectsPerUser;
  private final long minFreeDiskMb;
  private final Semaphore slot = new Semaphore(1);

  public AnalysisLimits(
      @Value("${app.limits.user-max-commits:500}") int userMaxCommits,
      @Value("${app.limits.admin-max-commits:70000}") int adminMaxCommits,
      @Value("${app.limits.max-projects-per-user:3}") int maxProjectsPerUser,
      @Value("${app.limits.min-free-disk-mb:2048}") long minFreeDiskMb) {
    this.userMaxCommits = userMaxCommits;
    this.adminMaxCommits = adminMaxCommits;
    this.maxProjectsPerUser = maxProjectsPerUser;
    this.minFreeDiskMb = minFreeDiskMb;
  }

  public boolean isAdmin(String role) {
    return ADMIN_ROLE.equals(role);
  }

  /** 依角色取得單次分析最多處理的 commit 數。 */
  public int maxCommitsFor(String role) {
    return isAdmin(role) ? adminMaxCommits : userMaxCommits;
  }

  public int getMaxProjectsPerUser() {
    return maxProjectsPerUser;
  }

  /** 目前專案數已達上限即視為超限；ADMIN 不受限。 */
  public boolean isProjectLimitReached(String role, long currentProjectCount) {
    return !isAdmin(role) && currentProjectCount >= maxProjectsPerUser;
  }

  /**
   * 非阻塞地取得全站唯一的分析槽位。
   *
   * @return true 表示取得；false 表示已有分析在執行
   */
  public boolean tryAcquire() {
    return slot.tryAcquire();
  }

  /** 釋放槽位，只應由成功取得者在 finally 中呼叫。 */
  public void release() {
    slot.release();
  }

  /**
   * 檢查 directory 所在磁碟的可用空間，低於門檻就丟出例外。目錄不存在時往上找最近的既有父目錄。
   *
   * @throws InsufficientDiskSpaceException 可用空間低於門檻
   */
  public void ensureDiskSpace(File directory) {
    File target = directory.getAbsoluteFile();
    while (target != null && !target.exists()) {
      target = target.getParentFile();
    }
    long usableMb = target == null ? 0 : target.getUsableSpace() / BYTES_PER_MB;
    if (usableMb < minFreeDiskMb) {
      throw new InsufficientDiskSpaceException(
          "伺服器磁碟空間不足（可用 " + usableMb + " MB，至少需要 " + minFreeDiskMb + " MB），請稍後再試");
    }
  }
}
