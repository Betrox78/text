-- 1589648471 DOWN invoice-indexes
DROP INDEX invoice_status_idx ON invoice;
DROP INDEX invoice_invoice_status_idx ON invoice;
DROP INDEX invoice_created_at_idx ON invoice;
DROP INDEX invoice_document_id_idx ON invoice;
DROP INDEX invoice_contpaq_id_idx ON invoice;
DROP INDEX invoice_contpaq_parcel_id_idx ON invoice;
DROP INDEX invoice_reference_idx ON invoice;
DROP INDEX invoice_is_global_idx ON invoice;
DROP INDEX invoice_init_valid_at_idx ON invoice;
DROP INDEX invoice_end_valid_at_idx ON invoice;
DROP INDEX invoice_global_period_idx ON invoice;
DROP INDEX invoice_purchase_origin_idx ON invoice;
DROP INDEX invoice_payment_condition_idx ON invoice;
DROP INDEX invoice_zip_code_idx ON invoice;
DROP INDEX invoice_payment_method_idx ON invoice;
DROP INDEX invoice_cfdi_use_idx ON invoice;
DROP INDEX invoice_service_type_idx ON invoice;