-- 1562708665 UP add service and action columns in expense_concept table
DELETE FROM expense_concept WHERE id = 3;

ALTER TABLE expense_concept
ADD COLUMN service enum('boardingpass','parcel','rental') DEFAULT NULL AFTER description,
ADD COLUMN action enum('purchase','income','change','cancel','expense','withdrawal','return','voucher') DEFAULT NULL AFTER service,
ADD UNIQUE expense_concept_service_action_idx(service, action);

UPDATE expense_concept SET service = 'rental', action = 'cancel' WHERE id = 1;
UPDATE expense_concept SET service = 'rental', action = 'return' WHERE id = 2;
UPDATE expense_concept SET service = 'parcel', action = 'cancel' WHERE id = 4;