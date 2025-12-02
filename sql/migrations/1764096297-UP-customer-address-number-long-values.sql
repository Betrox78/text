-- 1764096297 UP customer-address-number-long-values
ALTER TABLE customer_addresses
  MODIFY COLUMN no_ext VARCHAR(30) NULL;

ALTER TABLE customer_addresses
  MODIFY COLUMN no_int VARCHAR(30) NULL;