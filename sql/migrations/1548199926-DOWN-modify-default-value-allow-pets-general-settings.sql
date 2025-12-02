-- 1548199926 DOWN modify-default-value-allow-pets-general-settings
UPDATE general_setting SET value = '1', value_default = '1' WHERE FIELD = 'allow_pets';