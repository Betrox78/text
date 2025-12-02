-- 1567707622 UP insert-default-value-expires-at-in-general-settings
UPDATE general_setting SET value = '365' WHERE FIELD = 'abierto_expires_days';