-- 1548808240 UP add-alias-column-in-payment-method-table
ALTER TABLE payment_method
ADD COLUMN alias enum('cash', 'card','check','transfer','deposit') NOT NULL DEFAULT 'cash' AFTER is_cash;