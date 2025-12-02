-- 1558829340 DOWN add-branchoffice-id-and-purchase-origin
ALTER TABLE invoice 
DROP COLUMN purchase_origin,
DROP FOREIGN KEY invoice_branchoffice_id,
DROP COLUMN branchoffice_id,
DROP INDEX invoice_branchoffice_id_idx;