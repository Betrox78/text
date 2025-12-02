-- 1548808714 DOWN update-where-name-tarjeta-set-alias-card-in-payment-method-table
UPDATE payment_method SET is_cash = 1, alias = 'cash' WHERE name LIKE 'Tarjeta%';