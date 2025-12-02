-- 1758549982 UP add-new-available-cols-to-pc-det
ALTER TABLE `payment_complement_detail`
  ADD COLUMN `prev_invoice_subtotal` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `installment_number`,
  ADD COLUMN `new_invoice_subtotal` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `prev_invoice_subtotal`,
  ADD COLUMN `prev_invoice_iva` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `new_debt_amount`,
  ADD COLUMN `new_invoice_iva` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `iva_amount`,
  ADD COLUMN `prev_invoice_iva_withheld` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `new_invoice_iva`,
  ADD COLUMN `new_invoice_iva_withheld` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `iva_withheld_amount`;