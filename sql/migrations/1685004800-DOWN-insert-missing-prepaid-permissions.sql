-- permisos para el submodulo app.prepaid_packages
delete from permission where name = '#list' and description = 'Ver listado' and sub_module_id = 135;
delete from permission where name = '.update' and description = 'Modificar' and sub_module_id = 135;
delete from permission where name = '#delete' and description = 'Eliminar' and sub_module_id = 135;