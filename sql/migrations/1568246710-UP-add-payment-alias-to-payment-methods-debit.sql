-- 1568246710 UP add-payment-alias-to-payment-methods-debit
ALTER TABLE payment_method
MODIFY COLUMN alias enum('cash','card','check','transfer','deposit', 'debit');

UPDATE payment_method SET name = 'Tarjeta de crédito' WHERE name = 'Tarjeta (crédito/débito)';

INSERT INTO payment_method (name, is_cash, alias, icon, created_by) VALUES ('Tarjeta de débito', 0, 'debit','icon-credit-card', 1);
