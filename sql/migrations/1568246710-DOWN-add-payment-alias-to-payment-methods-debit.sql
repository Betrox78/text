-- 1568246710 DOWN add-payment-alias-to-payment-methods-debit

DELETE FROM payment_method WHERE alias = 'debit';

UPDATE payment_method SET name = 'Tarjeta (crédito/débito)' WHERE name = 'Tarjeta de crédito';

ALTER TABLE payment_method
MODIFY COLUMN alias enum('cash','card','check','transfer','deposit');
