-- 1557806200 UP add-customer-billing-id-to-invoice
ALTER TABLE invoice 
ADD COLUMN customer_billing_information_id INT(11) NULL,
ADD INDEX invoice_customer_billing_information_id_idx (customer_billing_information_id ASC),
ADD CONSTRAINT invoice_customer_billing_information_id
  FOREIGN KEY (customer_billing_information_id)
  REFERENCES customer_billing_information (id)
  ON DELETE RESTRICT
  ON UPDATE RESTRICT;