-- 1570147343 UP update delivered at in parcels table
UPDATE parcels p SET p.delivered_at = (SELECT created_at FROM parcels_packages_tracking WHERE parcel_id = p.id AND action = 'delivered' LIMIT 1);