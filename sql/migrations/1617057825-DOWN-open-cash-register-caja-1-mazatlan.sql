-- 1617057825 DOWN open-cash-register-caja-1-mazatlan
update cash_out set cash_out_status = 1 where id = 7824;

update cash_registers set cash_out_status = 1 where branchoffice_id = 4 and cash_register = "Caja 1";