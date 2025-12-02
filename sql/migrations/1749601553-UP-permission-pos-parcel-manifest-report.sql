-- 1749601553 UP permission pos parcel-manifest report
INSERT INTO sub_module(id, name, module_id, group_type, created_by) VALUES
(156, 'app.report.parcels-manifest', 2, 'reports', 1);

INSERT INTO permission(name, description, dependency_id, sub_module_id, multiple, created_by) VALUES
('#list', 'Ver lista', null, 156, 0, 1);