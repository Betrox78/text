-- 1718779611 DOWN add customer billing information id column in parcels prepaid table
ALTER TABLE parcels_prepaid
DROP CONSTRAINT fk_parcels_prepaid_customer_billing_information_id,
DROP COLUMN customer_billing_information_id;