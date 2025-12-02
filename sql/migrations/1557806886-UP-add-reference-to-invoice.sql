-- 1557806886 UP add-reference-to-invoice
ALTER TABLE invoice
ADD COLUMN reference VARCHAR(60) NULL;