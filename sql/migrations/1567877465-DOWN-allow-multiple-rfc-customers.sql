-- 1567877465 DOWN allow-multiple-rfc-customers
ALTER TABLE customer_billing_information
DROP INDEX customer_billing_information_customer_id_rfc_zip_code_no_ext;

CREATE UNIQUE INDEX customer_billing_information_rfc_zip_code_no_ext
ON customer_billing_information(rfc, zip_code, no_ext);