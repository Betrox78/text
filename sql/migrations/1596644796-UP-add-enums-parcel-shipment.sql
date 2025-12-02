-- 1596644796 UP add-enums-parcel-shipment
ALTER TABLE parcels
MODIFY COLUMN
shipment_type enum('OCU','EAD','RAD/OCU','RAD/EAD') DEFAULT 'OCU'