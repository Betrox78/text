-- 1745198404 UP add-index-for-customer-serv-qury
CREATE INDEX idx_pp_parcel_id_weight ON parcels_packages(parcel_id, weight);

CREATE INDEX idx_tlccp_specific_invoice ON travel_logs_ccp(specific_parcel_id, invoice_status);
CREATE INDEX idx_tlccp_travel_log_invoice ON travel_logs_ccp(travel_log_id, invoice_status);

CREATE INDEX idx_dp_status_parcel_id ON debt_payment(status, parcel_id);

CREATE INDEX idx_payment_status_parcel_id ON payment(status, parcel_id);