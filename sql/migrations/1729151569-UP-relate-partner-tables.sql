-- 1729151569 UP relate-partner-tables
ALTER TABLE integration_partner_session
ADD COLUMN integration_partner_api_key_id INT(11) DEFAULT NULL AFTER integration_partner_id;

CREATE INDEX idx_integration_partner_session_api_key_id ON integration_partner_session (integration_partner_api_key_id);