-- 1744822978 UP add-admin-origins-for-cards
UPDATE payment_method SET allow_origin = 'pos, miticket, admin' WHERE alias = 'card' AND id > 0;
UPDATE payment_method SET allow_origin = 'pos, miticket, admin' WHERE alias = 'debit' AND id > 0;
UPDATE payment_method SET allow_origin = '' WHERE alias = 'codi' AND id > 0;