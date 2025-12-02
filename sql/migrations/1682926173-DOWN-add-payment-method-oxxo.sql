-- 1682926173 DOWN add-payment-method-oxxo
DELETE FROM payment_method WHERE alias = 'oxxo';
ALTER TABLE payment_method MODIFY COLUMN alias ENUM('cash', 'card', 'check', 'transfer', 'deposit', 'debit', 'codi');
ALTER TABLE payment MODIFY COLUMN payment_method ENUM('cash', 'card', 'check', 'transfer', 'deposit', 'debit', 'codi');
UPDATE payment_method SET allow_origin = 'pos' WHERE alias = 'card' OR alias = 'debit';