insert into permission (name , description, sub_module_id , created_by)
    values ('#list' , 'Ver listado', 135,1);

insert into permission (name , description, sub_module_id , created_by)
    values ('.update' , 'Modificar', 135,1);

insert into permission (name , description, sub_module_id , created_by)
    values ('#delete' , 'Eliminar', 135,1);

update permission set description = "Registrar", name = "#create" where id = 271;