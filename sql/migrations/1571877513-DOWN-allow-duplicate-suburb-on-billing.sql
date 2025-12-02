-- 1571877513 DOWN allow-duplicate-suburb-on-billing
ALTER TABLE customer_billing_information
DROP INDEX customer_billing_customer_id_rfc_zip_code_no_ext_suburb_id;

CREATE UNIQUE INDEX customer_billing_information_customer_id_rfc_zip_code_no_ext
ON customer_billing_information(customer_id, rfc, zip_code, no_ext);