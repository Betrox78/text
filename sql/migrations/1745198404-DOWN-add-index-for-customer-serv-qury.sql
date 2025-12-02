-- 1745198404 DOWN add-index-for-customer-serv-qury
DROP INDEX idx_pp_parcel_id_weight ON parcels_packages;

DROP INDEX idx_tlccp_specific_invoice ON travel_logs_ccp;
DROP INDEX idx_tlccp_travel_log_invoice ON travel_logs_ccp;

DROP INDEX idx_dp_status_parcel_id ON debt_payment;

DROP INDEX idx_payment_status_parcel_id ON payment;