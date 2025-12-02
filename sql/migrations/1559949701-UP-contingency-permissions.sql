-- 1559949701 UP contingency permissions


INSERT INTO sub_module (id,name,module_id,group_type,menu_type,created_by) 
VALUES (73, 'app.contingency', 3, 'parcel', null, 1);


INSERT INTO permission (id, name, description,sub_module_id,dependency_id, created_by) 
  VALUES (171,'#add', 'Registrar entrada por contingencia ',73,null,1);
