-- 1758392329 UP add-billing-name-rfc-to-invoices
ALTER TABLE invoice
    ADD COLUMN billing_name VARCHAR(255) NULL AFTER email,
    ADD COLUMN rfc VARCHAR(13) NULL AFTER billing_name,
    ADD INDEX invoice_billing_name_idx (billing_name),
    ADD INDEX invoice_rfc_idx (rfc);
