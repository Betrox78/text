-- 1682975032 DOWN integration-token-on-boarding-pass
DROP INDEX idx_boarding_pass_integration_partner_session_id 
ON boarding_pass;

ALTER TABLE boarding_pass
DROP COLUMN integration_partner_session_id;

