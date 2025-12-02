-- 1574882449 DOWN add-pdf-xml-document-on-invoices-table
ALTER TABLE invoice
    DROP COLUMN media_document_xml_name,
    DROP COLUMN media_document_pdf_name;