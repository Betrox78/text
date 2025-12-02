-- 1751076911 DOWN update group type ops submodules
UPDATE sub_module SET group_type = 'reports' WHERE name = 'app.daily-logs';
UPDATE sub_module SET group_type = 'travels_log' WHERE name = 'app.shipment_pending_to_load';
UPDATE sub_module SET group_type = 'travels_log' WHERE name = 'app.shipment_pending_to_download';
UPDATE sub_module SET group_type = 'reports' WHERE name = 'app.shipment_load_history';
UPDATE sub_module SET group_type = 'reports' WHERE name = 'app.shipment_download_history';
UPDATE sub_module SET group_type = 'travels_log' WHERE name = 'app.shipment_parcels_pending_to_load';
UPDATE sub_module SET group_type = 'travels_log' WHERE name = 'app.shipment_parcels_pending_to_download';
UPDATE sub_module SET group_type = 'reports' WHERE name = 'app.reports.parcels-manifest';
UPDATE sub_module SET group_type = 'reports' WHERE name = 'app.parcels-manifest';