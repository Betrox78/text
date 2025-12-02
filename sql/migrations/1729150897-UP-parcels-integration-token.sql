-- 1729150897 UP parcels-integration-token
ALTER TABLE parcels
ADD COLUMN integration_partner_session_id INT(11) DEFAULT NULL;

CREATE INDEX idx_parcels_integration_partner_session_id ON parcels (integration_partner_session_id);
