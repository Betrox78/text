-- 1548201642 UP modify-group-type-frozen-cost-and-pets-cost-in-general-setting-table
UPDATE general_setting SET group_type = 'pets' WHERE FIELD = 'pets_cost';
UPDATE general_setting SET group_type = 'frozen' WHERE FIELD = 'frozen_cost';