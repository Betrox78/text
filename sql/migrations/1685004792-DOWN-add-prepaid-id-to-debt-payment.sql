-- 1685004792 DOWN
ALTER TABLE debt_payment
DROP COLUMN prepaid_travel_id;
DROP INDEX `debt_payment_prepaid_travel_id`;
