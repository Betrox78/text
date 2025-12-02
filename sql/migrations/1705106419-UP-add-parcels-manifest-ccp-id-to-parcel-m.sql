-- 1705106419 UP add-parcels-manifest-ccp-id-to-parcel-m
ALTER TABLE parcels_manifest ADD COLUMN parcels_manifest_ccp_id int(11) DEFAULT NULL;
ALTER TABLE parcels_manifest
ADD CONSTRAINT `fk_parcels_manifest_parcels_manifest_ccp_id`
FOREIGN KEY (parcels_manifest_ccp_id) REFERENCES `parcels_manifest_ccp` (`id`);