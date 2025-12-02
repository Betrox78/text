-- 1743509488 UP add-payment-complement-cols-to-invoice
ALTER TABLE `invoice`
ADD COLUMN `cfdi_payment_form` VARCHAR(45) NULL DEFAULT NULL AFTER `uuid`,
ADD COLUMN `cfdi_payment_method` ENUM('PUE', 'PPD') NULL DEFAULT NULL AFTER `cfdi_payment_form`,
ADD COLUMN `available_amount_for_complement` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `cfdi_payment_method`;
