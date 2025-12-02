-- 1751213999 DOWN add-invoice-date-to-invoice
ALTER TABLE invoice
  DROP INDEX invoice_invoice_date_idx,
  DROP COLUMN invoice_date;