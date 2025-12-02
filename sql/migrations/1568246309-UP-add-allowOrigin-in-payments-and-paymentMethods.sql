-- 1568246309 UP add-allowOrigin-in-payments-and-paymentMethods
ALTER TABLE payment_method
    ADD COLUMN allow_origin VARCHAR(100) DEFAULT 'pos' AFTER icon;

INSERT INTO payment_method(name, is_cash, alias, allow_origin, icon, created_by) VALUES('Cheque', 0, 'check', 'admin', 'icon-briefcase-checked', 1);
INSERT INTO payment_method(name, is_cash, alias, allow_origin,  icon, created_by) VALUES('Transferencia', 0, 'transfer', 'admin', 'icon-inbox', 1);
INSERT INTO payment_method(name, is_cash, alias, allow_origin, icon, created_by) VALUES('Depósito', 0, 'deposit', 'admin', 'icon-receipt', 1);
UPDATE payment_method SET allow_origin = 'pos, admin' WHERE alias = 'card';