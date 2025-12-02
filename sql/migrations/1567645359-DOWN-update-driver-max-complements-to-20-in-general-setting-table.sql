-- 1567645359 DOWN update driver max complements to 20 in general setting table
UPDATE general_setting SET value = '2', value_default = '2' WHERE FIELD = 'driver_max_complements';