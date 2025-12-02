-- 1548203732 DOWN add-allow-frozen-in-general-setting-table
DELETE FROM general_setting WHERE FIELD = 'allow_frozen' AND group_type = 'frozen';