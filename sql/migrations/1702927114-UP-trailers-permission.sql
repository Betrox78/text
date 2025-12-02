-- 1702927114 UP trailers permission
INSERT INTO sub_module(id, name, module_id, group_type, menu_type, created_by)
VALUES(145, 'app.trailers', 1, 'operation', 'o_sub_catalogue', 1);

INSERT into permission (name, description, sub_module_id,created_by)
VALUES ('#list', 'Catalogo de remolques', 145 ,1);