-- 1548201642 DOWN modify-group-type-frozen-cost-and-pets-cost-in-general-setting-table
UPDATE general_setting SET group_type = null WHERE FIELD = 'pets_cost';
UPDATE general_setting SET group_type = 'general' WHERE FIELD = 'frozen_cost';