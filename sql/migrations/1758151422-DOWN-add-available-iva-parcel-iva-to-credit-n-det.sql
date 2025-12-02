-- 1758151422 DOWN add-available-iva-parcel-iva-to-credit-n-det
ALTER TABLE `credit_note_detail`
  DROP COLUMN `new_invoice_total_amount`,
  DROP COLUMN `prev_invoice_total_amount`,
  DROP COLUMN `new_invoice_iva_withheld`,
  DROP COLUMN `prev_invoice_iva_withheld`,
  DROP COLUMN `new_invoice_iva`,
  DROP COLUMN `prev_invoice_iva`,
  DROP COLUMN `new_invoice_subtotal`,
  DROP COLUMN `prev_invoice_subtotal`,
  ADD COLUMN `prev_available_amount_for_complement` DECIMAL(12,2) NULL DEFAULT 0.00 AFTER `total_amount`,
  ADD COLUMN `new_available_amount_for_complement`  DECIMAL(12,2) NULL DEFAULT 0.00 AFTER `prev_available_amount_for_complement`;