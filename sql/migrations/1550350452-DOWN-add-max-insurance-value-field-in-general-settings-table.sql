-- 1550350452 DOWN add-max-insurance-value-field-in-general-settings-table
DELETE FROM general_setting WHERE FIELD = 'max_insurance_value' AND group_type = 'parcel';