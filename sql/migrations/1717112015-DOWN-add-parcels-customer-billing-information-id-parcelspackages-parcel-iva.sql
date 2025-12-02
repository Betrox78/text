-- 1717112015 DOWN add parcels customer billing information id parcelspackages parcel iva
ALTER TABLE parcels
DROP CONSTRAINT fk_parcels_customer_billing_information_id,
DROP COLUMN customer_billing_information_id;

ALTER TABLE parcels_packages
DROP COLUMN parcel_iva;