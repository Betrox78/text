-- 1571677307 UP add apply only first purchase column in promos table
ALTER TABLE promos
ADD COLUMN apply_only_first_purchase TINYINT(1) DEFAULT 0 AFTER available_days;
