-- 1753451335 DOWN add-serie-folio-to-invoice
ALTER TABLE `invoice`
  DROP COLUMN `cfdi_folio`,
  DROP COLUMN `cfdi_serie`;