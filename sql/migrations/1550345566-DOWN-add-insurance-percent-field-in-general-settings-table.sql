-- 1550345566 DOWN add-insurance-percent-field-in-general-settings-table
DELETE FROM general_setting WHERE FIELD = 'insurance_percent' AND group_type = 'parcel';