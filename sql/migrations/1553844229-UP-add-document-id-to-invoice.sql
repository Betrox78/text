-- 1553844229 UP add-document-id-to-invoice
ALTER TABLE invoice
ADD COLUMN document_id VARCHAR(512) DEFAULT NULL;