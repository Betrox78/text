-- 1718017502 UP create table customer customer billing info
CREATE TABLE customer_customer_billing_info(
	id INTEGER(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
	customer_id INTEGER(11) NOT NULL,
    customer_billing_information_id INTEGER(11) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE KEY unique_customer_customer_billing_info (customer_id, customer_billing_information_id),
    CONSTRAINT customer_customer_billing_info_customer_id FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT customer_customer_billing_info_customer_billing_information_id FOREIGN KEY (customer_billing_information_id) REFERENCES customer_billing_information(id) ON DELETE NO ACTION ON UPDATE NO ACTION
);