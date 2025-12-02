-- 1589332611 DOWN add-codi-to-cashout
ALTER TABLE `cash_out`
DROP COLUMN `codi`;

ALTER TABLE `cash_out_detail`
MODIFY COLUMN `denomination_type` ENUM('cash', 'voucher', 'check', 'transfer', 'deposit');
