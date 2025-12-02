-- 1549658232 DOWN add-field-authorization-codes-vigency-in-general-settings-table
DELETE FROM general_setting WHERE FIELD = 'authorization_codes_vigency' AND group_type = 'general';