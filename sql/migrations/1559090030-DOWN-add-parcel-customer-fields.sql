-- 1559090030 DOWN add-parcel-customer-fields
ALTER TABLE customer_billing_information
DROP COLUMN contpaq_parcel_status,
DROP COLUMN contpaq_id,
DROP COLUMN contpaq_parcel_id;