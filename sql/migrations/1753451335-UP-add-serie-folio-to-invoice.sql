-- 1753451335 UP add-serie-folio-to-invoice
ALTER TABLE `invoice`
ADD COLUMN `cfdi_serie` VARCHAR(45) NULL DEFAULT NULL AFTER `uuid`,
ADD COLUMN `cfdi_folio` INT NULL DEFAULT NULL AFTER `cfdi_serie`;
