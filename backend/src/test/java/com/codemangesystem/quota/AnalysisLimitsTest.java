package com.codemangesystem.quota;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnalysisLimitsTest {

  private final AnalysisLimits limits = new AnalysisLimits(500, 70000, 3, 0);

  @Test
  @DisplayName("ADMIN 用 admin 上限；USER、DEMO、缺少 role 都用 user 上限")
  void maxCommitsByRole() {
    assertEquals(70000, limits.maxCommitsFor("ADMIN"));
    assertEquals(500, limits.maxCommitsFor("USER"));
    assertEquals(500, limits.maxCommitsFor("DEMO"));
    assertEquals(500, limits.maxCommitsFor(null));
    assertEquals(500, limits.maxCommitsFor("admin"));
  }

  @Test
  @DisplayName("專案數：達上限即超限，ADMIN 不受限")
  void projectLimit() {
    assertFalse(limits.isProjectLimitReached("USER", 2));
    assertTrue(limits.isProjectLimitReached("USER", 3));
    assertTrue(limits.isProjectLimitReached(null, 4));
    assertFalse(limits.isProjectLimitReached("ADMIN", 100));
    assertEquals(3, limits.getMaxProjectsPerUser());
  }

  @Test
  @DisplayName("槽位：第二次取得失敗，釋放後可再取得")
  void slot() {
    assertTrue(limits.tryAcquire());
    assertFalse(limits.tryAcquire());
    limits.release();
    assertTrue(limits.tryAcquire());
  }

  @Test
  @DisplayName("槽位：另一個執行緒在佔用時立即取得失敗，不阻塞")
  void slotAcrossThreads() throws Exception {
    assertTrue(limits.tryAcquire());
    AtomicBoolean other = new AtomicBoolean(true);
    CountDownLatch done = new CountDownLatch(1);
    new Thread(() -> {
      other.set(limits.tryAcquire());
      done.countDown();
    }).start();
    assertTrue(done.await(5, java.util.concurrent.TimeUnit.SECONDS));
    assertFalse(other.get());
  }

  @Test
  @DisplayName("磁碟檢查：門檻為 0 通過，門檻過大丟出例外；目錄不存在時看父目錄")
  void diskSpace(@TempDir Path tempDir) {
    limits.ensureDiskSpace(tempDir.toFile());
    limits.ensureDiskSpace(new File(tempDir.toFile(), "not/exist/yet"));

    AnalysisLimits strict = new AnalysisLimits(500, 70000, 3, Long.MAX_VALUE);
    AnalysisLimits.InsufficientDiskSpaceException e = assertThrows(
        AnalysisLimits.InsufficientDiskSpaceException.class,
        () -> strict.ensureDiskSpace(tempDir.toFile()));
    assertTrue(e.getMessage().contains("磁碟空間不足"));
  }
}
