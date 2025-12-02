-- 1574281131 DOWN add-payment-methods-on-cashOut
ALTER TABLE cash_out
    DROP COLUMN checks,
    DROP COLUMN transfer,
    DROP COLUMN deposit;