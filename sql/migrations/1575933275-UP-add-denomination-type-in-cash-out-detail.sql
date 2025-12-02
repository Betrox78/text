-- 1575933275 UP add-denomination-type-in-cash-out-detail
ALTER TABLE cash_out_detail
ADD COLUMN denomination_type ENUM('cash', 'voucher', 'check', 'transfer', 'deposit');