-- 1552606075 UP drop-rfc-unique-customer-billing-information
CREATE INDEX customer_billing_information_rfc
ON customer_billing_information(rfc);

ALTER TABLE customer_billing_information
MODIFY COLUMN no_ext VARCHAR(15) NOT NULL;

UPDATE customer_billing_information SET zip_code=0 WHERE zip_code IS NULL;
ALTER TABLE customer_billing_information
MODIFY COLUMN zip_code int(11) NOT NULL;

CREATE UNIQUE INDEX customer_billing_information_rfc_zip_code_no_ext
ON customer_billing_information(rfc, zip_code, no_ext);