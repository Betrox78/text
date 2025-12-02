-- 1554580507 DOWN add-branchoffice-parcels
ALTER TABLE parcels
DROP FOREIGN KEY parcels_branchoffice_id_fk_idx;

ALTER TABLE parcels
DROP COLUMN branchoffice_id;