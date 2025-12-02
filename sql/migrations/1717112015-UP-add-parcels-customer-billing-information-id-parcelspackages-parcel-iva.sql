-- 1717112015 UP add parcels customer billing information id parcelspackages parcel iva
ALTER TABLE parcels
ADD COLUMN customer_billing_information_id INTEGER DEFAULT NULL AFTER customer_id,
ADD CONSTRAINT fk_parcels_customer_billing_information_id FOREIGN KEY (customer_billing_information_id) REFERENCES customer_billing_information (id) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE parcels_packages
ADD COLUMN parcel_iva DECIMAL(12, 2) DEFAULT 0.0 AFTER iva;