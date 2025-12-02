-- 1758151422 UP add-available-iva-parcel-iva-to-credit-n-det
ALTER TABLE `credit_note_detail`
    DROP COLUMN `prev_available_amount_for_complement`,
    DROP COLUMN `new_available_amount_for_complement`,
    ADD COLUMN `prev_invoice_subtotal` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `total_amount`,
    ADD COLUMN `new_invoice_subtotal` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `prev_invoice_subtotal`,
    ADD COLUMN `prev_invoice_iva` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `new_invoice_subtotal`,
    ADD COLUMN `new_invoice_iva` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `prev_invoice_iva`,
    ADD COLUMN `prev_invoice_iva_withheld` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `new_invoice_iva`,
    ADD COLUMN `new_invoice_iva_withheld` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `prev_invoice_iva_withheld`,
    ADD COLUMN `prev_invoice_total_amount` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `new_invoice_iva_withheld`,
    ADD COLUMN `new_invoice_total_amount` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `prev_invoice_total_amount`;