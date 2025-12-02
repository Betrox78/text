-- 1737657241 UP add-missing-index-for-debt-payment
CREATE INDEX idx_debt_payment_parcel_id ON debt_payment(parcel_id);
CREATE INDEX idx_debt_payment_parcel_prepaid_id ON debt_payment(parcel_prepaid_id);