-- 1555461157 DOWN add extra earning percent in general setting table
DELETE FROM general_setting WHERE FIELD = 'extra_earning_percent' AND group_type = 'rental';