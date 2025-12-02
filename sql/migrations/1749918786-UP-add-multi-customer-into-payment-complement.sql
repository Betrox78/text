-- 1749918786 UP add-multi-customer-into-payment-complement
ALTER TABLE payment_complement
ADD COLUMN is_multi_customer BOOLEAN NOT NULL DEFAULT FALSE;