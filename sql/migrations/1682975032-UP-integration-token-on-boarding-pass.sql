-- 1682975032 UP integration-token-on-boarding-pass
ALTER TABLE boarding_pass
ADD COLUMN integration_partner_session_id INT(11) DEFAULT NULL;

CREATE INDEX idx_boarding_pass_integration_partner_session_id ON boarding_pass (integration_partner_session_id);