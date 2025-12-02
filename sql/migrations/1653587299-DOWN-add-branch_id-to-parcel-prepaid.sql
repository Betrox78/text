-- 1653587299 DOWN add-branch_id-to-parcel-prepaid
ALTER TABLE parcels_prepaid
DROP FOREIGN KEY prepaid_branchoffice_id_fk_idx;

ALTER TABLE parcels_prepaid
DROP  KEY prepaid_branchoffice_id_fk_idx;

ALTER TABLE parcels_prepaid
DROP  COLUMN branchoffice_id;
