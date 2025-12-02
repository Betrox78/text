-- 1738297096 UP integration-partner-customer-seed
INSERT IGNORE INTO integration_partner_customer (
    id, customer_code, integration_partner_id, name, last_name, phone, email
)
SELECT DISTINCT 
    NULL AS id, 
    MD5(
      CONCAT(
        UPPER(integration_partner_session.integration_partner_id), 
        UPPER(parcels.sender_name), 
        UPPER(parcels.sender_last_name), 
        UPPER(parcels.sender_phone), 
        UPPER(parcels.sender_email)
      )
    ) AS customer_code,
    integration_partner_session.integration_partner_id AS integration_partner_id, 
    parcels.sender_name AS name, 
    parcels.sender_last_name AS last_name, 
    parcels.sender_phone AS phone, 
    parcels.sender_email AS email
FROM parcels
INNER JOIN integration_partner_session ON parcels.integration_partner_session_id = integration_partner_session.id
WHERE parcels.integration_partner_session_id IS NOT NULL;
