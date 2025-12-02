-- 1587509608 UP suburb-add-indexes
CREATE INDEX suburb_name_idx ON suburb(name);
CREATE INDEX suburb_zip_code_idx ON suburb(zip_code);
CREATE INDEX suburb_status_idx ON suburb(status);
CREATE INDEX suburb_created_at_idx ON suburb(created_at);
