-- 1543082336 UP add-checklist-charges-rental

ALTER TABLE rental
ADD COLUMN checklist_charges decimal(12,2) DEFAULT 0.0 AFTER driver_cost;