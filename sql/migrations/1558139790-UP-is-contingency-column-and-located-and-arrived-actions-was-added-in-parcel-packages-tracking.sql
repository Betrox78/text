-- 1558139790 UP is contingency column and located and arrived actions was added in parcel packages tracking
ALTER TABLE parcels_packages_tracking
ADD COLUMN is_contingency tinyint(1) NOT NULL DEFAULT false AFTER terminal_id,
MODIFY COLUMN action enum(
'register', 'paid', 'move', 'intransit', 'loaded', 'downloaded',
'incidence', 'canceled', 'closed', 'printed', 'delivered',
'deliveredcancel', 'located', 'arrived') NOT NULL AFTER ticket_id;