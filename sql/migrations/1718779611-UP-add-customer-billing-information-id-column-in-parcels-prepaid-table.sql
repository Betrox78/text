-- 1718779611 UP add customer billing information id column in parcels prepaid table
ALTER TABLE parcels_prepaid
ADD customer_billing_information_id INT DEFAULT NULL AFTER customer_id,
ADD CONSTRAINT fk_parcels_prepaid_customer_billing_information_id FOREIGN KEY (customer_billing_information_id)
	REFERENCES customer_billing_information(id) ON UPDATE NO ACTION ON DELETE NO ACTION;