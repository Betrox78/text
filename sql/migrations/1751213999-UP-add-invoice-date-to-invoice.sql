-- 1751213999 UP add-invoice-date-to-invoice
ALTER TABLE invoice
  ADD COLUMN invoice_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD INDEX invoice_invoice_date_idx (invoice_date);