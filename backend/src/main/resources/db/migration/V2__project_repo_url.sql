-- V2：專案識別改用 repo URL（ADR 0001，docs/adr/0001-project-identity.md）
-- 注意：這是破壞性 migration，會清空 project、files、method、diff_info、personal_info 五張表，
--       舊的分析資料不做遷移，需重新分析。user 表與各 *_seq 表不動。
-- 套用後請手動刪除 backend/src/cloneCode/ 底下舊的 clone 資料夾（保留 .gitkeep），
-- 否則 GitCloner 會走 pull，並找不到已被清掉的 Project。
-- repo_url 先允許 NULL（UNIQUE 允許多筆 NULL），待寫入 URL 的步驟完成後再另行設為 NOT NULL。

-- 依外鍵順序清空：子表在前
DELETE FROM `diff_info`;
DELETE FROM `method`;
DELETE FROM `files`;
DELETE FROM `personal_info`;
DELETE FROM `project`;

ALTER TABLE `project`
  ADD COLUMN `repo_url` varchar(255) DEFAULT NULL,
  ADD CONSTRAINT `uk_project_repo_url` UNIQUE (`repo_url`);
