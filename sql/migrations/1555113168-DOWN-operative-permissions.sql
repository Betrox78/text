-- 1555113168 DOWN operative permissions
DELETE FROM permission WHERE id=144 OR id=141 OR id=142 OR id=143;
DELETE FROM sub_module WHERE id=59 OR id=56 OR id=57 OR id=58;

ALTER TABLE sub_module
MODIFY COLUMN travels_log ENUM('general', 'admin', 'operation', 'logistic', 'vans', 'parcel', 'risks', 'reports', 'config', 'buses');
