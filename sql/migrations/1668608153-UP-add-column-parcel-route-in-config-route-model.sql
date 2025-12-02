-- 1668608153 UP add column parcel route in config route model
ALTER TABLE config_route
ADD COLUMN parcel_route BOOLEAN DEFAULT FALSE AFTER expire_at;
