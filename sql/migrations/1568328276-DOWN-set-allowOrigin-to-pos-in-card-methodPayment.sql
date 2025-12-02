-- 1568328276 DOWN set-allowOrigin-to-pos-in-card-methodPayment
UPDATE payment_method SET allow_origin = 'pos, admin' WHERE alias = 'card';