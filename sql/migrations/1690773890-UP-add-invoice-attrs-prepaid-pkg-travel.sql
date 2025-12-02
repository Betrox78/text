-- 1690773890 UP add-invoice-attrs-prepaid-pkg-travel
ALTER TABLE prepaid_package_travel
ADD COLUMN has_invoice tinyint(1) NOT NULL DEFAULT '0',
ADD COLUMN invoice_is_global BIT(1) NOT NULL DEFAULT 0,
ADD COLUMN invoice_id INT NULL,
ADD KEY prepaid_package_travel_invoice_id_fk_key (invoice_id),
ADD CONSTRAINT prepaid_package_travel_invoice_id_fk FOREIGN KEY (invoice_id) REFERENCES invoice (id) ON DELETE RESTRICT ON UPDATE RESTRICT;

