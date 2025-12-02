-- 1758392329 DOWN add-billing-name-rfc-to-invoices
ALTER TABLE invoice
  DROP INDEX invoice_billing_name_idx,
  DROP INDEX invoice_rfc_idx,
  DROP COLUMN rfc,
  DROP COLUMN billing_name;