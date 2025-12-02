-- 1550626372 UP add-insurance-id-in-parcels-table
ALTER TABLE parcels
ADD COLUMN insurance_id int(11) DEFAULT NULL,
ADD CONSTRAINT fk_parcels_insurances_id FOREIGN KEY (insurance_id) REFERENCES insurances(id);