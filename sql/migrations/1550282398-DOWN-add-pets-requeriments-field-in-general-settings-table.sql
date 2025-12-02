-- 1550282398 DOWN add-pets-requeriments-field-in-general-settings-table
DELETE FROM general_setting WHERE FIELD = 'pets_requirements' AND group_type = 'pets';