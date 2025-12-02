-- 1653587299 UP add-branch_id-to-parcel-prepaid
alter table parcels_prepaid
add column branchoffice_id int(11) DEFAULT NULL;


alter table parcels_prepaid
ADD CONSTRAINT `prepaid_branchoffice_id_fk_idx` FOREIGN KEY (`branchoffice_id`) REFERENCES `branchoffice` (`id`) ON DELETE CASCADE;