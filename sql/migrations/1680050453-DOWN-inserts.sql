-- 1680050453 DOWN inserts
-- permisos para el submodulo app.prepaid
delete from permission where name = '#create' and description = 'Venta de paquetes prepago' and sub_module_id = 136;

-- permisos para el submodulo app.reservation_prepaid
delete from permission where name = '#detail' and description = 'Ver detalle de reservación prepago' and sub_module_id = 137;
delete from permission where name = '#cancel' and description = 'Cancelación' and sub_module_id = 137;
delete from permission where name = '#print' and description = 'Re-impresiones' and sub_module_id = 137;
