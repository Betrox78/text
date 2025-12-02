-- 1547688860 DOWN add-field-allow-pets-in-general-settings-table
DELETE FROM general_setting where FIELD = 'allow_pets' AND group_type = 'pets';