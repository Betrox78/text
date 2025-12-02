-- 1731169670 UP add-payment_type_to_integration_partners
ALTER TABLE integration_partner
ADD COLUMN payment_type ENUM('agreement', 'credit', 'prepay') DEFAULT 'agreement';