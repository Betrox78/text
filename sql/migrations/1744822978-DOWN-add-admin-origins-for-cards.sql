-- 1744822978 DOWN add-admin-origins-for-cards
UPDATE payment_method SET allow_origin = 'pos, miticket' WHERE alias = 'card' AND id > 0;
UPDATE payment_method SET allow_origin = 'pos, miticket' WHERE alias = 'debit' AND id > 0;
UPDATE payment_method SET allow_origin = 'admin' WHERE alias = 'codi' AND id > 0;