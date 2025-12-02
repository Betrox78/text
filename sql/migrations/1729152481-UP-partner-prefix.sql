-- 1729152481 UP partner-prefix
ALTER TABLE integration_partner
ADD COLUMN prefix VARCHAR(10) DEFAULT NULL AFTER name;

CREATE INDEX idx_integration_partner_prefix ON integration_partner (prefix);