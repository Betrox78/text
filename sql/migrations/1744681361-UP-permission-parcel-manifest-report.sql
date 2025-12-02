-- 1744681361 UP permission parcel-manifest report
INSERT INTO sub_module(id, name, module_id, group_type, created_by) VALUES
(155, 'app.reports.parcels-manifest', 3, 'reports', 1);

INSERT INTO permission(name, description, dependency_id, sub_module_id, multiple, created_by) VALUES
('#list', 'Ver lista', null, 155, 0, 1);