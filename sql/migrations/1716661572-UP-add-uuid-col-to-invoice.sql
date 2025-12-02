-- 1716661572 UP add-uuid-col-to-invoice
ALTER TABLE invoice
ADD COLUMN uuid VARCHAR(50) NULL;