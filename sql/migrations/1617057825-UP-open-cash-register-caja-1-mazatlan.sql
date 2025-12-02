-- 1617057825 UP open-cash-register-caja-1-mazatlan
update cash_out set cash_out_status = 2 where cash_register_id = (select id from cash_registers where branchoffice_id = 4 and cash_register = "Caja 1")
and cash_out_status = 1;

update cash_registers set cash_out_status = 0 where branchoffice_id = 4 and cash_register = "Caja 1";