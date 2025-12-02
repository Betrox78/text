-- 1729152481 DOWN partner-prefix
DROP INDEX idx_integration_partner_prefix
ON integration_partner;

ALTER TABLE integration_partner
DROP COLUMN prefix;