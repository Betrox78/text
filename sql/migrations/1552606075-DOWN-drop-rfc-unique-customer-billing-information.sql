-- 1552606075 DOWN drop-rfc-unique-customer-billing-information
ALTER TABLE customer_billing_information
DROP INDEX customer_billing_information_rfc;

ALTER TABLE customer_billing_information
MODIFY COLUMN no_ext INT(11) NOT NULL;

ALTER TABLE customer_billing_information
MODIFY COLUMN zip_code int(11) DEFAULT NULL;

ALTER TABLE customer_billing_information
DROP INDEX customer_billing_information_rfc_zip_code_no_ext;