-- 1596644796 DOWN add-enums-parcel-shipment
ALTER TABLE parcels DROP COLUMN shipment_type;
ALTER TABLE parcels ADD shipment_type  enum('OCU','EAD') DEFAULT 'OCU'
