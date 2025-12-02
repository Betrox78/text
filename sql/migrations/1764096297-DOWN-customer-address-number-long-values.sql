-- 1764096297 DOWN customer-address-number-long-values
ALTER TABLE customer_addresses
  MODIFY COLUMN no_ext VARCHAR(15) NULL;

ALTER TABLE customer_addresses
  MODIFY COLUMN no_int VARCHAR(15) NULL;