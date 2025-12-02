-- 1589648471 UP invoice-indexes
CREATE INDEX invoice_status_idx ON invoice(status);
CREATE INDEX invoice_invoice_status_idx ON invoice(invoice_status);
CREATE INDEX invoice_created_at_idx ON invoice(created_at);
CREATE INDEX invoice_document_id_idx ON invoice(document_id);
CREATE INDEX invoice_contpaq_id_idx ON invoice(contpaq_id);
CREATE INDEX invoice_contpaq_parcel_id_idx ON invoice(contpaq_parcel_id);
CREATE INDEX invoice_reference_idx ON invoice(reference);
CREATE INDEX invoice_is_global_idx ON invoice(is_global);
CREATE INDEX invoice_init_valid_at_idx ON invoice(init_valid_at);
CREATE INDEX invoice_end_valid_at_idx ON invoice(end_valid_at);
CREATE INDEX invoice_global_period_idx ON invoice(global_period);
CREATE INDEX invoice_purchase_origin_idx ON invoice(purchase_origin);
CREATE INDEX invoice_payment_condition_idx ON invoice(payment_condition);
CREATE INDEX invoice_zip_code_idx ON invoice(zip_code);
CREATE INDEX invoice_payment_method_idx ON invoice(payment_method);
CREATE INDEX invoice_cfdi_use_idx ON invoice(cfdi_use);
CREATE INDEX invoice_service_type_idx ON invoice(service_type);

