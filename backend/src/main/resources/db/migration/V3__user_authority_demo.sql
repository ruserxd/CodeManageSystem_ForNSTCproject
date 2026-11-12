-- V3：新增 DEMO 角色（UserAuthority 的第三個值，ordinal = 2），放寬 user_authority 的 CHECK 從 0–1 到 0–2。
-- 既有資料不受影響：ADMIN = 0、USER = 1 的 ordinal 不變。
-- 注意：由 ddl-auto=update 建表後被 baseline 的舊庫，CHECK 名稱不一定是 user_chk_1，此時 DROP CHECK 會失敗。

ALTER TABLE `user` DROP CHECK `user_chk_1`;
ALTER TABLE `user` ADD CONSTRAINT `user_chk_1` CHECK (`user_authority` BETWEEN 0 AND 2);
