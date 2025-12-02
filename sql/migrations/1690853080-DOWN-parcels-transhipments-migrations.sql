-- 1690853080 DOWN parcels transhipments migrations
ALTER TABLE branchoffice
DROP COLUMN receive_transhipments,
DROP COLUMN transhipment_site_name;

ALTER TABLE parcels_packages_tracking
MODIFY COLUMN action ENUM('register', 'paid', 'move', 'intransit', 'loaded',
'downloaded', 'incidence', 'canceled', 'closed', 'printed', 'delivered',
'deliveredcancel', 'located', 'arrived', 'createdlog', 'canceledlog', 'ead', 'rad');

DROP TABLE parcels_transhipments;

DROP TABLE parcels_transhipments_history;

DROP TABLE branchoffice_parcel_receiving_config;