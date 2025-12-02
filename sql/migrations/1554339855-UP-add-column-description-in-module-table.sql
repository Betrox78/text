-- 1554339855 UP add column description in module table
ALTER TABLE module
ADD COLUMN description text AFTER name;

UPDATE module SET description = 'Lleve el control de las diversas terminales y centros de operación de allAbordo teniendo a su disposición las herramientas que lo habilitarán para gestionar desde la calendarización de rutas,  inventario, mercancía permitida, registro de clientes, asignación de permisos y restricciones a usuarios así como configuraciones generales. Manténgase informado de los movimientos de ingreso y egreso de cada uno de los centros de operaciones y genere reportes de ventas finales.' WHERE id = 1;
UPDATE module SET description = 'Desde el módulo de punto de venta contará con las opciones para venta de boletos, renta y cotización de vans, revisión de salidas próximas, realizar check in de los pasajeros, re imprimir boletos, así cómo en envío y recibo de paquetería.' WHERE id = 2;
UPDATE module SET description = 'Lleve el control de la logística de los centros de operación y punto de venta de allAbordo.' WHERE id = 3;