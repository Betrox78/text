-- 1751076474 DOWN add group types in submodule table
ALTER TABLE sub_module
MODIFY COLUMN group_type ENUM('general','admin','operation','logistic','vans','parcel','risks','reports','config','buses','travels_log','billing','guia_pp','prepaid','e-wallet') DEFAULT NULL;