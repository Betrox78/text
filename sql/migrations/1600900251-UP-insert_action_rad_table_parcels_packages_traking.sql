-- 1600900251 UP insert_action_rad_table_parcels_packages_traking
ALTER TABLE `parcels_packages_tracking`
CHANGE COLUMN `action` `action` ENUM('register', 'paid', 'move', 'intransit', 'loaded', 'downloaded', 'incidence', 'canceled', 'closed', 'printed', 'delivered', 'deliveredcancel', 'located', 'arrived', 'createdlog', 'canceledlog', 'ead', 'rad') NULL DEFAULT NULL ;
