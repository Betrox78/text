-- 1585784473 UP add-codi-enums
ALTER TABLE payment_method MODIFY COLUMN alias ENUM('cash', 'card', 'check', 'transfer', 'deposit', 'debit', 'codi');
UPDATE payment_method SET alias = 'codi', icon = 'icon-qrcode' where name = 'CODI';
ALTER TABLE payment MODIFY COLUMN payment_method ENUM('cash', 'card', 'check', 'transfer', 'deposit', 'debit', 'codi');