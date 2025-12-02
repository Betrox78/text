-- 1568328276 UP set-allowOrigin-to-pos-in-card-methodPayment
UPDATE payment_method SET allow_origin = 'pos' WHERE alias = 'card';