-- V4：全站每日額度計數表（註冊、clone／分析）。
-- 每個（日期, 種類）一列，used 為當日已用次數；日期以 Asia/Taipei 為界，由應用程式寫入。
-- 扣額度以帶條件的 UPDATE 原子完成，不依賴讀出後比較。

CREATE TABLE `daily_quota` (
  `quota_date` date NOT NULL,
  `quota_kind` varchar(32) NOT NULL,
  `used` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`quota_date`, `quota_kind`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
