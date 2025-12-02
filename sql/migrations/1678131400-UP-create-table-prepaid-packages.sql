-- 1678131400 UP create-table-prepaid-packages
CREATE TABLE IF NOT EXISTS prepaid_package_travel(
id INT NOT NULL AUTO_INCREMENT ,
reservation_code varchar(60) NOT NULL,
amount decimal(12,2) NOT NULL DEFAULT 0.0,
total_amount decimal(12,2) NOT NULL DEFAULT 0.0,
created_by int(11) DEFAULT NULL,
created_at datetime DEFAULT NOW(),
updated_by int(11) DEFAULT NULL,
updated_at datetime DEFAULT NULL,
status tinyint(4) NOT NULL DEFAULT 1,
customer_id int(11) DEFAULT NULL,
employee_id int(11) DEFAULT NULL,
KEY fk_prepaid_customer_id (customer_id),
CONSTRAINT fk_prepaid_customer_id FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE CASCADE,
KEY fk_prepaid_employee_id (employee_id),
CONSTRAINT fk_prepaid_employee_id FOREIGN KEY (employee_id) REFERENCES employee (id) ON DELETE CASCADE,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
