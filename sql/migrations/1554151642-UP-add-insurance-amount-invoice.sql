-- 1554151642 UP add-insurance-amount-invoice
ALTER TABLE invoice
ADD COLUMN insurance_amount DOUBLE(12,2) DEFAULT 0.0;