-- 1722453432 DOWN alter table parcels packages scanner tracking add column shipment id
ALTER TABLE parcels_packages_scanner_tracking
DROP CONSTRAINT parcels_packages_scanner_tracking_pshipment_id_tk,
DROP COLUMN shipment_id;