-- 1555177773 UP add-contpaq-status
ALTER TABLE customer_billing_information
ADD COLUMN contpaq_status ENUM('pending','progress','done','error','expired') DEFAULT 'pending';