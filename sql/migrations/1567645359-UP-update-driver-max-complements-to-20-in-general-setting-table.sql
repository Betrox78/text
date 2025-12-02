-- 1567645359 UP update driver max complements to 20 in general setting table
UPDATE general_setting SET value = '20', value_default = '20' WHERE FIELD = 'driver_max_complements';