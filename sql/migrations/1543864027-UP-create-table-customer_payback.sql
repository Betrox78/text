-- 1543864027 UP create-table-customer_payback
CREATE TABLE IF NOT EXISTS customer_payback(
customer_id int(11) NOT NULL COMMENT 'ID del empleado',
currency_id int(11) NOT NULL COMMENT 'Id del currency base que esté especificado en la transacción',
points int(11) DEFAULT NULL COMMENT 'Puntos que tiene el cliente',
amount decimal(12,2) DEFAULT NULL COMMENT 'Importe en efectivo',
created_by int(11) DEFAULT NULL,
created_at datetime DEFAULT NOW(),
updated_by int(11) DEFAULT NULL,
updated_at datetime DEFAULT NULL,
CONSTRAINT fk_customer_payback_customer_id FOREIGN KEY (customer_id) REFERENCES customer(id),
CONSTRAINT fk_customer_payback_currency_id FOREIGN KEY (currency_id) REFERENCES currency(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;