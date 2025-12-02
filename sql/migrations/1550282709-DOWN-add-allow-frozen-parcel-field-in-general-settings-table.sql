-- 1550282709 DOWN add-allow-frozen-parcel-field-in-general-settings-table
DELETE FROM general_setting WHERE FIELD = 'allow_frozen_parcel' AND group_type = 'frozen';