-- 1560975501 UP add-parcel-status-and-id-for-invoices
ALTER TABLE invoice
ADD COLUMN contpaq_parcel_id INT(11) NULL AFTER contpaq_id;