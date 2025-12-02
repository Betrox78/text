-- 1690773890 DOWN add-invoice-attrs-prepaid-pkg-travel
ALTER TABLE prepaid_package_travel
DROP FOREIGN KEY prepaid_package_travel_invoice_id_fk,
DROP KEY prepaid_package_travel_invoice_id_fk_key,
DROP COLUMN has_invoice,
DROP COLUMN invoice_is_global,
DROP COLUMN invoice_id;