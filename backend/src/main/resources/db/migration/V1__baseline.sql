-- V1：導入 Flyway 時的基準 schema
-- 內容由 Hibernate（ddl-auto=create）依當時的 entity 在 MySQL 上建表後匯出。
-- 已經由 ddl-auto=update 建好表的資料庫會被 baseline-on-migrate 標記在此版本，不會重新執行。

CREATE TABLE `user` (
  `user_authority` tinyint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `user_account` varchar(255) DEFAULT NULL,
  `user_email` varchar(255) DEFAULT NULL,
  `user_name` varchar(255) DEFAULT NULL,
  `user_password` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `user_chk_1` CHECK ((`user_authority` between 0 and 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `project` (
  `project_id` bigint NOT NULL,
  `head_revstr` varchar(255) DEFAULT NULL,
  `project_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `files` (
  `files_id` bigint NOT NULL,
  `project_id` bigint DEFAULT NULL,
  `file_name` varchar(255) DEFAULT NULL,
  `file_path` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`files_id`),
  KEY `FK1b63qab88c2fchx7iw6r1vh0l` (`project_id`),
  CONSTRAINT `FK1b63qab88c2fchx7iw6r1vh0l` FOREIGN KEY (`project_id`) REFERENCES `project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `method` (
  `files_id` bigint DEFAULT NULL,
  `method_id` bigint NOT NULL,
  `method_name` text,
  PRIMARY KEY (`method_id`),
  KEY `FKlkbkr4q6hbpo13c0vnrle3l3b` (`files_id`),
  CONSTRAINT `FKlkbkr4q6hbpo13c0vnrle3l3b` FOREIGN KEY (`files_id`) REFERENCES `files` (`files_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `diff_info` (
  `commit_time` datetime(6) DEFAULT NULL,
  `diff_info_id` bigint NOT NULL,
  `method_id` bigint DEFAULT NULL,
  `author` varchar(255) DEFAULT NULL,
  `author_email` varchar(255) DEFAULT NULL,
  `commit_message` text,
  `diff_code` text,
  `head_revstr` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`diff_info_id`),
  KEY `FK3q9p6efpb0ro3n4g172c94078` (`method_id`),
  CONSTRAINT `FK3q9p6efpb0ro3n4g172c94078` FOREIGN KEY (`method_id`) REFERENCES `method` (`method_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `personal_info` (
  `personal_info_id` bigint NOT NULL,
  `project_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `head_revstr` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`personal_info_id`),
  KEY `FKmyfsfqybk4flrgowx6lief96y` (`project_id`),
  KEY `FK9lk7kc3eams2ytqcpiu72h0fw` (`user_id`),
  CONSTRAINT `FK9lk7kc3eams2ytqcpiu72h0fw` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `FKmyfsfqybk4flrgowx6lief96y` FOREIGN KEY (`project_id`) REFERENCES `project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- GenerationType.AUTO 在 MySQL 上由 Hibernate 以資料表模擬 sequence，初始值為 1
CREATE TABLE `user_seq` (`next_val` bigint DEFAULT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `project_seq` (`next_val` bigint DEFAULT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `files_seq` (`next_val` bigint DEFAULT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `method_seq` (`next_val` bigint DEFAULT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `diff_info_seq` (`next_val` bigint DEFAULT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE `personal_info_seq` (`next_val` bigint DEFAULT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `user_seq` VALUES (1);
INSERT INTO `project_seq` VALUES (1);
INSERT INTO `files_seq` VALUES (1);
INSERT INTO `method_seq` VALUES (1);
INSERT INTO `diff_info_seq` VALUES (1);
INSERT INTO `personal_info_seq` VALUES (1);
