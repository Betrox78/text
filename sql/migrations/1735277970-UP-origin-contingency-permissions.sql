-- 1735277970 UP origin contingency permissions
INSERT INTO sub_module (id,name,module_id,group_type,menu_type,created_by)
VALUES (154, 'app.origin-contingency', 3, 'parcel', null, 1);

INSERT INTO permission (name, description,sub_module_id,dependency_id, created_by)
  VALUES ('#add', 'Registrar origen por contingencia', 154, null, 1);