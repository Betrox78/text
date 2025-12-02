-- 1751076911 UP update group type ops submodules
UPDATE sub_module SET group_type = 'first_mile' WHERE name = 'app.daily-logs';
UPDATE sub_module SET group_type = 'first_mile' WHERE name = 'app.shipment_pending_to_load';
UPDATE sub_module SET group_type = 'first_mile' WHERE name = 'app.shipment_pending_to_download';
UPDATE sub_module SET group_type = 'first_mile' WHERE name = 'app.shipment_load_history';
UPDATE sub_module SET group_type = 'first_mile' WHERE name = 'app.shipment_download_history';
UPDATE sub_module SET group_type = 'first_mile' WHERE name = 'app.shipment_parcels_pending_to_load';
UPDATE sub_module SET group_type = 'first_mile' WHERE name = 'app.shipment_parcels_pending_to_download';
UPDATE sub_module SET group_type = 'last_mile' WHERE name = 'app.reports.parcels-manifest';
UPDATE sub_module SET group_type = 'last_mile' WHERE name = 'app.parcels-manifest';