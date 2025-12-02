-- 1758549982 DOWN add-new-available-cols-to-pc-det
ALTER TABLE `payment_complement_detail`
  DROP COLUMN `new_invoice_iva_withheld`,
  DROP COLUMN `prev_invoice_iva_withheld`,
  DROP COLUMN `new_invoice_iva`,
  DROP COLUMN `prev_invoice_iva`,
  DROP COLUMN `new_invoice_subtotal`,
  DROP COLUMN `prev_invoice_subtotal`;