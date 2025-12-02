-- 1562708665 DOWN add service and action columns in expense_concept table
ALTER TABLE expense_concept
DROP INDEX expense_concept_service_action_idx,
DROP COLUMN service,
DROP COLUMN action;