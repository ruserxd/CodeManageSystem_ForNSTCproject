package com.codemangesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 用 Testcontainers 啟動一個空的 MySQL 並載入完整的 Spring context，
 * 驗證 Flyway 真的有執行 V1，而且 Hibernate 的 validate 能通過（entity 與 schema 一致）。
 *
 * <p>MySQL 版本由環境變數 TEST_MYSQL_IMAGE 決定（例如 mysql:8.4），沒設定時用和 docker-compose 相同的 mysql:8.0。
 * 沒有 Docker 的環境會跳過，要看 Surefire 統計的 Skipped 確認有實際執行。
 */
@Slf4j
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "app.jwt.secret=test-secret-test-secret-test-secret-123")
class FlywaySchemaValidationTest {

  private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse(mysqlImageName());

  @Container
  private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE);

  @Autowired
  private Environment environment;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private static String mysqlImageName() {
    String image = System.getenv("TEST_MYSQL_IMAGE");
    return image == null || image.isBlank() ? "mysql:8.0" : image;
  }

  /**
   * application.properties 的 spring.datasource.* 是自己參照自己，沒有環境變數時會出現 circular placeholder，
   * 所以一定要在這裡覆寫成容器的連線資訊
   */
  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
  }

  @Test
  @DisplayName("Hibernate 使用 application.properties 的 ddl-auto=validate，沒有被覆寫")
  void ddlAutoIsValidateTest() {
    assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
  }

  @Test
  @DisplayName("Flyway 在空的資料庫上真的執行了 V1（type 是 SQL 而不是 BASELINE）")
  void baselineMigrationExecutedTest() {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history"
            + " WHERE version = '1' AND type = 'SQL' AND success = 1",
        Integer.class);
    assertEquals(1, count);
  }

  @Test
  @DisplayName("連到的 MySQL 版本和 TEST_MYSQL_IMAGE 指定的 tag 一致")
  void mysqlVersionMatchesImageTagTest() {
    String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
    String tag = MYSQL_IMAGE.getVersionPart();
    log.info("測試用的 MySQL image：{}，SELECT VERSION()：{}",
        MYSQL_IMAGE.asCanonicalNameString(), version);

    // 加上 "." 避免 8.4 誤配到 8.40 之類的版本
    assertTrue(version.equals(tag) || version.startsWith(tag + "."),
        "MySQL 版本 " + version + " 和 image tag " + tag + " 不一致");
  }
}
