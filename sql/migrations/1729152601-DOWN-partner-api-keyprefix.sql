-- 1729152601 DOWN partner-api-keyprefix
DROP INDEX idx_integration_partner_api_key_prefix
ON integration_partner_api_key;

ALTER TABLE integration_partner_api_key
DROP COLUMN prefix;