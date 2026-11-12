package com.codemangesystem.quota;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 全站每日額度。日期以 Asia/Taipei 為界，檢查與扣除合併成單一條件式 UPDATE，並發時不會超賣。
 *
 * <p>本類別不加 @Transactional：呼叫端沒有交易時每次扣除立即提交（分析額度「失敗也算」），
 * 呼叫端在交易內時則加入該交易（註冊額度與 user 寫入一起提交或回滾）。
 */
@Service
public class QuotaService {

  public static final ZoneId ZONE = ZoneId.of("Asia/Taipei");

  /** 額度種類，name() 即存入 daily_quota.quota_kind 的值。 */
  public enum Kind {
    REGISTRATION,
    ANALYSIS
  }

  private final JdbcTemplate jdbcTemplate;
  private final Clock clock;
  private final int dailyRegistrations;
  private final int dailyAnalyses;

  @Autowired
  public QuotaService(
      JdbcTemplate jdbcTemplate,
      @Value("${app.quota.daily-registrations:5}") int dailyRegistrations,
      @Value("${app.quota.daily-analyses:10}") int dailyAnalyses) {
    this(jdbcTemplate, Clock.system(ZONE), dailyRegistrations, dailyAnalyses);
  }

  /** 測試用：可注入固定的 Clock。 */
  public QuotaService(
      JdbcTemplate jdbcTemplate, Clock clock, int dailyRegistrations, int dailyAnalyses) {
    this.jdbcTemplate = jdbcTemplate;
    this.clock = clock;
    this.dailyRegistrations = dailyRegistrations;
    this.dailyAnalyses = dailyAnalyses;
  }

  /**
   * 嘗試扣一次今日額度。
   *
   * @return true 表示扣除成功；false 表示今日額度已用完
   */
  public boolean tryAcquire(Kind kind) {
    LocalDate today = LocalDate.now(clock.withZone(ZONE));
    // 確保當日有一列；已存在時不改動 used
    jdbcTemplate.update(
        "INSERT INTO daily_quota (quota_date, quota_kind, used) VALUES (?, ?, 0) "
            + "ON DUPLICATE KEY UPDATE used = used",
        today,
        kind.name());
    int updated =
        jdbcTemplate.update(
            "UPDATE daily_quota SET used = used + 1 "
                + "WHERE quota_date = ? AND quota_kind = ? AND used < ?",
            today,
            kind.name(),
            limitOf(kind));
    return updated == 1;
  }

  private int limitOf(Kind kind) {
    return kind == Kind.REGISTRATION ? dailyRegistrations : dailyAnalyses;
  }
}
