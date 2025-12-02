-- 1758149508 UP add-available-iva-parcel-iva-to-invoices
ALTER TABLE `invoice`
ADD COLUMN `available_subtotal_for_complement` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `cfdi_payment_method`,
ADD COLUMN `available_iva_for_complement` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `available_subtotal_for_complement`,
ADD COLUMN `available_iva_withheld_for_complement` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `available_iva_for_complement`;