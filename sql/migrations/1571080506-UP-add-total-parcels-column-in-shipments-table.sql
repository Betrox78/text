-- 1571080506 UP add total parcels column in shipments table
ALTER TABLE shipments
ADD COLUMN total_parcels INT(11) DEFAULT 0 AFTER total_complements;