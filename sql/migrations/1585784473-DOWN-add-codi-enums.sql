-- 1585784473 DOWN add-codi-enums
UPDATE payment_method SET alias = '', icon = 'icon-cached' where name = 'CODI';
ALTER TABLE payment_method MODIFY COLUMN alias ENUM('cash', 'card', 'check', 'transfer', 'deposit', 'debit');
ALTER TABLE payment MODIFY COLUMN payment_method ENUM('cash', 'card', 'check', 'transfer', 'deposit', 'debit');