-- 1705106419 DOWN add-parcels-manifest-ccp-id-to-parcel-m
ALTER TABLE parcels_manifest
DROP FOREIGN KEY fk_parcels_manifest_parcels_manifest_ccp_id;

ALTER TABLE parcels_manifest
DROP COLUMN parcels_manifest_ccp_id;