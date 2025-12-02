-- 1554851963 UP add column prefix in cash out
ALTER TABLE cash_registers
ADD COLUMN prefix VARCHAR(10) DEFAULT NULL after branchoffice_id;