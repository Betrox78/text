-- 1558139790 DOWN is contingency column and located and arrived actions was added in parcel packages tracking
ALTER TABLE parcels_packages_tracking
DROP COLUMN is_contingency,
MODIFY COLUMN action enum(
'register', 'paid', 'move', 'intransit', 'loaded', 'downloaded',
'incidence', 'canceled', 'closed', 'printed', 'delivered',
'deliveredcancel') NOT NULL AFTER ticket_id;