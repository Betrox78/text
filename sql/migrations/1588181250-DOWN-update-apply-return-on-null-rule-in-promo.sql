-- 1588181250 DOWN update-apply-return-on-null-rule-in-promo
UPDATE promos SET apply_return = true WHERE rule IS NULL;