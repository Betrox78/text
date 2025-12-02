-- 1699526470 UP add submodule and permission ticket prices rules
INSERT INTO sub_module(id, name, module_id, group_type, menu_type, created_by)
VALUES(144, 'app.ticket_prices_rules', 1, 'operation', 'o_sub_catalogue', 1);

INSERT into permission (name, description, sub_module_id,created_by)
VALUES ('#list', 'Reglas precios boletos', 144 ,1);