-- 1548199926 UP modify-default-value-allow-pets-general-settings
UPDATE general_setting SET value = '0', value_default = '0' WHERE FIELD = 'allow_pets';