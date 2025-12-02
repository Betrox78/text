-- 1568246309 DOWN add-allowOrigin-in-payments-and-paymentMethods
ALTER TABLE payment_method
    DROP COLUMN allow_origin;

DELETE FROM payment_method WHERE alias = 'check';
DELETE FROM payment_method WHERE alias = 'transfer';
DELETE FROM payment_method WHERE alias = 'deposit';