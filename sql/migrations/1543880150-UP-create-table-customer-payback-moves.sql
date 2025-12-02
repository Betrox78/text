-- 1543880150 UP
CREATE TABLE IF NOT EXISTS customer_payback_moves(
customer_id int(11) NOT NULL COMMENT 'ID del empleado',
type_movement enum('I','O') NOT NULL COMMENT 'Tipo de movimiento I:In, O: Out',
currency_id int(11) NOT NULL COMMENT 'Id del currency base que esté especificado en la transacción',
points int(11) DEFAULT NULL COMMENT 'Puntos que se le ingresan o se le quitan a la cuenta del cliente',
amount decimal(12,2) DEFAULT NULL COMMENT 'Importe en efectivo',
motive text NOT NULL COMMENT 'Motivo por el cual se registra el movimiento: Venta de boleto, renta, paqueteria (los 3 generan puntos)
cancelaciones (por cualquier motivo de los anteriores. Reembolso',
id_parent int(11) DEFAULT NULL COMMENT 'Id del la relación de donde viene el importe por venta paqueteria renta, etc.',
created_by int(11) DEFAULT NULL,
created_at datetime DEFAULT NOW(),
updated_by int(11) DEFAULT NULL,
updated_at datetime DEFAULT NULL,
CONSTRAINT fk_customer_payback_moves_customer_id FOREIGN KEY (customer_id) REFERENCES customer(id),
CONSTRAINT fk_customer_payback_moves_currency_id FOREIGN KEY (currency_id) REFERENCES currency(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;