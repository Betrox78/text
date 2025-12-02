-- 1745865561 UP add-show-note-on-invoice-customer
ALTER TABLE customer
ADD COLUMN parcel_notes_on_invoice BOOLEAN NOT NULL DEFAULT FALSE AFTER relate_ccp_with_invoice;
