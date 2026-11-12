package com.codemangesystem.quota;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codemangesystem.quota.QuotaService.Kind;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 用 Testcontainers 的真實 MySQL 驗證每日額度：上限前後、跨日歸零、並發不超賣。
 * 沒有 Docker 的環境會跳過，要看 Surefire 統計的 Skipped 確認有實際執行。
 */
@Testcontainers(disabledWithoutDocker = true)
class QuotaServiceTest {

  @Container
  private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
      DockerImageName.parse(mysqlImageName()));

  private static HikariDataSource dataSource;
  private static JdbcTemplate jdbcTemplate;

  private final MutableClock clock = new MutableClock();

  private static String mysqlImageName() {
    String image = System.getenv("TEST_MYSQL_IMAGE");
    return image == null || image.isBlank() ? "mysql:8.0" : image;
  }

  @BeforeAll
  static void setUpDatabase() {
    dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(MYSQL.getJdbcUrl());
    dataSource.setUsername(MYSQL.getUsername());
    dataSource.setPassword(MYSQL.getPassword());
    dataSource.setMaximumPoolSize(30);
    Flyway.configure().dataSource(dataSource).load().migrate();
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @AfterAll
  static void tearDownDatabase() {
    if (dataSource != null) {
      dataSource.close();
    }
  }

  @BeforeEach
  void cleanTable() {
    jdbcTemplate.update("DELETE FROM daily_quota");
    clock.set("2026-11-12T10:00:00Z");
  }

  private QuotaService service(int registrations, int analyses) {
    return new QuotaService(jdbcTemplate, clock, registrations, analyses);
  }

  private int used(String date, Kind kind) {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT used FROM daily_quota WHERE quota_date = ? AND quota_kind = ?",
        Integer.class, date, kind.name());
    return count == null ? 0 : count;
  }

  @Test
  @DisplayName("分析：第 N 次通過、第 N+1 次被拒，計數停在 N")
  void analysisLimitTest() {
    QuotaService quota = service(5, 3);
    for (int i = 0; i < 3; i++) {
      assertTrue(quota.tryAcquire(Kind.ANALYSIS), "第 " + (i + 1) + " 次應通過");
    }
    assertFalse(quota.tryAcquire(Kind.ANALYSIS));
    assertFalse(quota.tryAcquire(Kind.ANALYSIS));
    assertEquals(3, used("2026-11-12", Kind.ANALYSIS));
  }

  @Test
  @DisplayName("註冊：第 N 次通過、第 N+1 次被拒，且與分析額度各自計算")
  void registrationLimitTest() {
    QuotaService quota = service(2, 10);
    assertTrue(quota.tryAcquire(Kind.REGISTRATION));
    assertTrue(quota.tryAcquire(Kind.REGISTRATION));
    assertFalse(quota.tryAcquire(Kind.REGISTRATION));
    assertTrue(quota.tryAcquire(Kind.ANALYSIS));
    assertEquals(2, used("2026-11-12", Kind.REGISTRATION));
    assertEquals(1, used("2026-11-12", Kind.ANALYSIS));
  }

  @Test
  @DisplayName("上限為 0 時一律拒絕")
  void zeroLimitTest() {
    assertFalse(service(0, 0).tryAcquire(Kind.ANALYSIS));
  }

  @Test
  @DisplayName("跨日歸零：台北 23:59 用滿，00:00 可再通過，舊日期的列不被覆寫")
  void rolloverTest() {
    QuotaService quota = service(5, 2);
    // 台北 2026-11-12 23:59 = UTC 15:59
    clock.set("2026-11-12T15:59:00Z");
    assertTrue(quota.tryAcquire(Kind.ANALYSIS));
    assertTrue(quota.tryAcquire(Kind.ANALYSIS));
    assertFalse(quota.tryAcquire(Kind.ANALYSIS));

    // 台北 2026-11-13 00:00 = UTC 16:00
    clock.set("2026-11-12T16:00:00Z");
    assertTrue(quota.tryAcquire(Kind.ANALYSIS));
    assertEquals(2, used("2026-11-12", Kind.ANALYSIS));
    assertEquals(1, used("2026-11-13", Kind.ANALYSIS));
  }

  @Test
  @DisplayName("並發不超賣：50 個執行緒同時扣、上限 10，成功數恰為 10")
  void concurrentNoOversellTest() throws Exception {
    int threads = 50;
    int limit = 10;
    QuotaService quota = service(5, limit);
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch go = new CountDownLatch(1);
    List<Future<Boolean>> results = new ArrayList<>();
    try {
      for (int i = 0; i < threads; i++) {
        Callable<Boolean> task = () -> {
          ready.countDown();
          go.await();
          return quota.tryAcquire(Kind.ANALYSIS);
        };
        results.add(pool.submit(task));
      }
      ready.await();
      go.countDown();
      int success = 0;
      for (Future<Boolean> result : results) {
        if (result.get()) {
          success++;
        }
      }
      assertEquals(limit, success);
      assertEquals(limit, used("2026-11-12", Kind.ANALYSIS));
    } finally {
      pool.shutdownNow();
    }
  }

  /** 可手動調整時間的 Clock，時區固定用 UTC，由 QuotaService 自行換成 Asia/Taipei。 */
  private static final class MutableClock extends Clock {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);

    void set(String instant) {
      now.set(Instant.parse(instant));
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return Clock.fixed(now.get(), zone);
    }

    @Override
    public Instant instant() {
      return now.get();
    }
  }
}
