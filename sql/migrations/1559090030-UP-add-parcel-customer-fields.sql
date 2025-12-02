-- 1559090030 UP add-parcel-customer-fields
ALTER TABLE customer_billing_information
ADD COLUMN contpaq_parcel_status enum('pending','progress','done','error','expired') DEFAULT 'pending',
ADD COLUMN contpaq_id INT(11) NULL,
ADD COLUMN contpaq_parcel_id INT(11) NULL;