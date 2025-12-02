-- 1574281131 UP add-payment-methods-on-cashOut
ALTER table cash_out
    ADD COLUMN checks FLOAT(12, 2) NULL AFTER vouchers,
    ADD COLUMN transfer FLOAT(12, 2) NULL AFTER vouchers,
    ADD COLUMN deposit FLOAT(12, 2) NULL AFTER vouchers;