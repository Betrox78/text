-- 1548201288 UP delete-duplicate-frozen-cost-field-in-general-setting-table
DELETE FROM general_setting where FIELD = 'frozen_cost' AND group_type = 'frozen';