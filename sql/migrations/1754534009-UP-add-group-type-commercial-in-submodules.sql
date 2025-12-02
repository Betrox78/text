-- 1754534009 UP add group type commercial in submodules
ALTER TABLE sub_module
MODIFY COLUMN group_type ENUM('general','admin','operation','logistic','vans','parcel','risks','reports','config','buses','travels_log','billing','guia_pp','prepaid','e-wallet','first_mile','last_mile', 'commercial') DEFAULT NULL;