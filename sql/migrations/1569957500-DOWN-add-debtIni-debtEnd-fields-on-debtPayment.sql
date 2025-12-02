-- 1569957500 DOWN add-debtIni-debtEnd-fields-on-debtPayment
ALTER TABLE debt_payment DROP COLUMN debt_end;
ALTER TABLE debt_payment DROP COLUMN debt_ini;