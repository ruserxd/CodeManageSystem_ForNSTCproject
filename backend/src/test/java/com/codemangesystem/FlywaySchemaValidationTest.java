package com.codemangesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 用 Testcontainers 啟動一個空的 MySQL 並載入完整的 Spring context，
 * 驗證 Flyway 真的有執行 V1、V2，而且 Hibernate 的 validate 能通過（entity 與 schema 一致）。
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
  @DisplayName("Flyway 在空的資料庫上真的執行了 V2")
  void projectRepoUrlMigrationExecutedTest() {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history"
            + " WHERE version = '2' AND type = 'SQL' AND success = 1",
        Integer.class);
    assertEquals(1, count);
  }

  @Test
  @DisplayName("project.repo_url 是可為 NULL 的 varchar(255)，且有只含 repo_url 的唯一索引")
  void repoUrlColumnAndUniqueKeyTest() {
    List<String> column = jdbcTemplate.queryForList(
        "SELECT CONCAT(COLUMN_TYPE, '/', IS_NULLABLE) FROM information_schema.COLUMNS"
            + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project'"
            + " AND COLUMN_NAME = 'repo_url'",
        String.class);
    assertEquals(List.of("varchar(255)/YES"), column);

    List<String> indexColumns = jdbcTemplate.queryForList(
        "SELECT COLUMN_NAME FROM information_schema.STATISTICS"
            + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project'"
            + " AND INDEX_NAME = 'uk_project_repo_url' AND NON_UNIQUE = 0",
        String.class);
    assertEquals(List.of("repo_url"), indexColumns);
  }

  @Test
  @DisplayName("repo_url 允許多筆 NULL，但相同的非 NULL 值會被唯一鍵擋下")
  void repoUrlUniqueBehaviorTest() {
    try {
      jdbcTemplate.update("INSERT INTO project (project_id, project_name) VALUES (9001, 'n1')");
      jdbcTemplate.update("INSERT INTO project (project_id, project_name) VALUES (9002, 'n2')");

      jdbcTemplate.update(
          "INSERT INTO project (project_id, repo_url) VALUES (9003, 'https://example.com/a/b')");
      assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
          "INSERT INTO project (project_id, repo_url) VALUES (9004, 'https://example.com/a/b')"));
    } finally {
      jdbcTemplate.update("DELETE FROM project WHERE project_id BETWEEN 9001 AND 9004");
    }
  }

  @Test
  @DisplayName("V1 有資料的資料庫升級到最新版後，5 張分析資料表被清空，user 表保留")
  void upgradeFromV1ClearsAnalysisDataTest() {
    JdbcTemplate db = newDatabase("upgrade_v1");
    migrate("upgrade_v1", "1", false);
    insertFullChain(db);

    migrate("upgrade_v1", null, false);

    assertAnalysisTablesEmpty(db);
    assertEquals(1, count(db, "user"));
  }

  @Test
  @DisplayName("baseline 過的舊資料庫（flyway_schema_history 為空）也能套用 V2")
  void upgradeBaselinedDatabaseTest() {
    JdbcTemplate db = newDatabase("upgrade_baseline");
    migrate("upgrade_baseline", "1", false);
    insertFullChain(db);
    // 模擬由 ddl-auto=update 建好表、沒有 flyway_schema_history 的舊資料庫
    db.execute("DROP TABLE flyway_schema_history");

    migrate("upgrade_baseline", null, true);

    assertEquals(1, db.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history"
            + " WHERE version = '1' AND type = 'BASELINE' AND success = 1", Integer.class));
    assertEquals(1, db.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history"
            + " WHERE version = '2' AND type = 'SQL' AND success = 1", Integer.class));
    assertAnalysisTablesEmpty(db);
    assertEquals(1, count(db, "user"));
  }

  private static String jdbcUrl(String database) {
    return "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MySQLContainer.MYSQL_PORT)
        + "/" + database;
  }

  /** 用 root 在同一個容器內建立空的 database（Testcontainers 的 root 密碼與 getPassword() 相同） */
  private static JdbcTemplate newDatabase(String database) {
    new JdbcTemplate(new DriverManagerDataSource(jdbcUrl(""), "root", MYSQL.getPassword()))
        .execute("CREATE DATABASE " + database);
    return new JdbcTemplate(
        new DriverManagerDataSource(jdbcUrl(database), "root", MYSQL.getPassword()));
  }

  private static void migrate(String database, String target, boolean baselineOnMigrate) {
    var config = Flyway.configure()
        .dataSource(jdbcUrl(database), "root", MYSQL.getPassword())
        .baselineOnMigrate(baselineOnMigrate)
        .baselineVersion("1");
    if (target != null) {
      config.target(target);
    }
    config.load().migrate();
  }

  private static void insertFullChain(JdbcTemplate db) {
    db.update("INSERT INTO `user` (user_id, user_account) VALUES (1, 'u')");
    db.update("INSERT INTO project (project_id, project_name) VALUES (1, 'p')");
    db.update("INSERT INTO files (files_id, project_id, file_name) VALUES (1, 1, 'f')");
    db.update("INSERT INTO method (method_id, files_id, method_name) VALUES (1, 1, 'm')");
    db.update("INSERT INTO diff_info (diff_info_id, method_id) VALUES (1, 1)");
    db.update("INSERT INTO personal_info (personal_info_id, project_id, user_id)"
        + " VALUES (1, 1, 1)");
  }

  private static void assertAnalysisTablesEmpty(JdbcTemplate db) {
    for (String table : List.of("diff_info", "method", "files", "personal_info", "project")) {
      assertEquals(0, count(db, table), table + " 應該被清空");
    }
  }

  private static int count(JdbcTemplate db, String table) {
    return db.queryForObject("SELECT COUNT(*) FROM `" + table + "`", Integer.class);
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
