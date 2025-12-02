-- 1574882449 UP add-pdf-xml-document-on-invoices-table
ALTER TABLE invoice
    ADD COLUMN media_document_xml_name VARCHAR(512) NULL AFTER document_id,
    ADD COLUMN media_document_pdf_name VARCHAR(512) NULL AFTER document_id;