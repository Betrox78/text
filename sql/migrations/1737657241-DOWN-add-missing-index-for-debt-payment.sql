-- 1737657241 DOWN add-missing-index-for-debt-payment
DROP INDEX idx_debt_payment_parcel_id ON debt_payment;
DROP INDEX idx_debt_payment_parcel_prepaid_id ON debt_payment;