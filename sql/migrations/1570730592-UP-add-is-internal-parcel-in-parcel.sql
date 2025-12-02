-- 1570730592 UP add-is-internal-parcel-in-parcel
ALTER TABLE parcels
ADD COLUMN is_internal_parcel boolean DEFAULT false;

