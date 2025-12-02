-- 1588181250 UP update-apply-return-on-null-rule-in-promo
UPDATE promos SET apply_return = false WHERE rule IS NULL;