-- 1583949746 DOWN update-file-name-extension-parcels-signature-image
UPDATE parcels_deliveries SET signature = SUBSTRING_INDEX(signature, '.', 1) WHERE signature IS NOT NULL;