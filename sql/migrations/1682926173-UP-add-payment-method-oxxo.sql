-- 1682926173 UP add-payment-method-oxxo
ALTER TABLE payment_method MODIFY COLUMN alias ENUM('cash', 'card', 'check', 'transfer', 'deposit', 'debit', 'codi', 'oxxo');
ALTER TABLE payment MODIFY COLUMN payment_method ENUM('cash', 'card', 'check', 'transfer', 'deposit', 'debit', 'codi', 'oxxo');
INSERT INTO payment_method (name, alias, allow_origin, created_by) VALUES ('OXXO', 'oxxo', 'miticket', 1);
UPDATE payment_method SET allow_origin = 'pos, miticket' WHERE alias = 'card' OR alias = 'debit';