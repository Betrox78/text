-- 1589332611 UP add-codi-to-cashout
ALTER TABLE `cash_out`
ADD COLUMN `codi` FLOAT(12,2) NULL AFTER `cash_register_id`;

ALTER TABLE `cash_out_detail`
MODIFY COLUMN `denomination_type` ENUM('cash', 'voucher', 'check', 'transfer', 'deposit', 'codi');