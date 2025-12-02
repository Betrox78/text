-- 1738297104 UP integration-partner-customer-address-seed
INSERT IGNORE INTO integration_partner_customer_address (
    id, integration_partner_customer_id, customer_address_id
)
SELECT DISTINCT 
    NULL AS id, 
    integration_partner_customer.id AS integration_partner_customer_id,
    parcels.sender_address_id AS customer_address_id
FROM parcels
INNER JOIN integration_partner_session ON parcels.integration_partner_session_id = integration_partner_session.id
INNER JOIN integration_partner_customer ON  integration_partner_customer.integration_partner_id = integration_partner_session.integration_partner_id
INNER JOIN integration_partner ON integration_partner.id = integration_partner_session.integration_partner_id
WHERE parcels.integration_partner_session_id IS NOT NULL
AND parcels.sender_id = integration_partner.customer_id;
