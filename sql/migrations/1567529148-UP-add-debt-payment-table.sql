-- 1567529148 UP add-debt-payment-table
CREATE TABLE debt_payment(
id int(11) NOT NULL AUTO_INCREMENT,
payment_id int(11) NOT NULL,
customer_id int(11) NOT NULL,
parcel_id int(11) NULL,
rental_id int(11) NULL,
boarding_pass_id int(11) null,
amount float(12,2) NOT NULL,
status TINYINT(4) NOT NULL DEFAULT 1,
created_by INT NOT NULL,
created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_by INT NULL,
updated_at DATETIME NULL,
PRIMARY KEY (id)
)ENGINE=InnoDB CHARSET=utf8;