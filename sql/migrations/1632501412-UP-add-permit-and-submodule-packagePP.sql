-- 1632501412 UP add-permit-and-submodule-packagePP

insert into sub_module (id, name, module_id, group_type, menu_type, status, created_by )
values (107, "app.packagesPricesPP", 1, "operation", "o_sub_catalogue", 1,1);

insert into
 permission (id, name , description, dependency_id, sub_module_id, multiple, status, created_by)
values
(225, "#list", "Ver listado", null, 106, 0, 1, 1),
(226, "#editPricePP", "Modificar precios paquetes PP", 224, 106, 0, 1, 1),
(227, "#editPriceKmPP", "Modificar precios por KM", 224, 106, 0, 1, 1),
(228, "#addPricePP", "Agregar precios paquetes PP", 224, 106, 0, 1, 1),
(229, "#addPriceKmPP", "Agregar precios por KM", 224, 106, 0, 1, 1)