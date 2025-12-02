-- 1756916282 UP add-total-amount-to-credit-note-tables
ALTER TABLE `credit_note`
ADD COLUMN `total_amount` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `iva_withheld_amount`;

ALTER TABLE `credit_note_detail`
ADD COLUMN `total_amount` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `iva_withheld_amount`;