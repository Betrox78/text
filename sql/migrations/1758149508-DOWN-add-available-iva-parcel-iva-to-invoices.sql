-- 1758149508 DOWN add-available-iva-parcel-iva-to-invoices
ALTER TABLE `invoice`
  DROP COLUMN `available_iva_withheld_for_complement`,
  DROP COLUMN `available_iva_for_complement`,
  DROP COLUMN `available_subtotal_for_complement`;