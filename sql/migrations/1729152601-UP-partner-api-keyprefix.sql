-- 1729152601 UP partner-api-keyprefix
ALTER TABLE integration_partner_api_key
ADD COLUMN prefix VARCHAR(10) DEFAULT NULL AFTER integration_partner_id;

CREATE INDEX idx_integration_partner_api_key_prefix ON integration_partner_api_key (prefix);