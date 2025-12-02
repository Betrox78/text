-- 1569957500 UP add-debtIni-debtEnd-fields-on-debtPayment
ALTER TABLE debt_payment
    ADD COLUMN debt_end FLOAT(12,2) DEFAULT 0 AFTER amount,
    ADD COLUMN debt_ini FLOAT(12,2) DEFAULT 0 AFTER amount;