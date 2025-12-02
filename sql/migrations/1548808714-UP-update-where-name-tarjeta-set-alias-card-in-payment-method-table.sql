-- 1548808714 UP update-where-name-tarjeta-set-alias-card-in-payment-method-table
UPDATE payment_method SET is_cash = 0, alias = 'card' WHERE name LIKE 'Tarjeta%';