-- 1557806200 DOWN add-customer-billing-id-to-invoice
ALTER TABLE invoice 
DROP FOREIGN KEY invoice_customer_billing_information_id,
DROP COLUMN customer_billing_information_id,
DROP INDEX invoice_customer_billing_information_id_idx;