-- 1729150897 DOWN parcels-integration-token
DROP INDEX idx_parcels_integration_partner_session_id
ON parcels;

ALTER TABLE parcels
DROP COLUMN integration_partner_session_id;
