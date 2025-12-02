-- 1541710001 UP seeds-initial

INSERT INTO `config_system` (`id`,`cron_exchange_rate_update_active`,`cron_exchange_rate_abordo_minus_qty`,`cron_exchange_rate_updated_last_time`,`status`,`created_at`,`created_by`,`updated_at`,`updated_by`)
VALUES (1,1,1.00,'2018-10-23',1,'2018-07-24 02:30:06',1,NULL,NULL);

INSERT INTO `config_vehicle` (`seatings`, `total_row`, `total_col`, `seat_by_row`, `seat_by_col`, `division_by_row`, `division_by_col`, `enumeration`, `enum_type`, `not_seats`, `emergency_exit`, `total_special_seats_handicapped`, `special_seats_handicapped`, `status`, `created_at`, `created_by`, `updated_at`, `updated_by`, `name`, `base`, `is_base`, `total_special_seats_women`, `special_seats_women`)
VALUES
	( 1, 11, 3, 0, 0, '', '1', '1', '1', '', '', 8, '1,1|1,2|1,3|1,4|2,4|2,3|2,2|2,1|', 1, '2018-08-24 22:29:03', 1, NULL, NULL, 'Configuración base - Autobus', 1, 0, 4, '3,1|3,2|3,3|3,4|');

INSERT INTO `config_vehicle` ( `seatings`, `total_row`, `total_col`, `seat_by_row`, `seat_by_col`, `division_by_row`, `division_by_col`, `enumeration`, `enum_type`, `not_seats`, `emergency_exit`, `total_special_seats_handicapped`, `special_seats_handicapped`, `status`, `created_at`, `created_by`, `updated_at`, `updated_by`, `name`, `base`, `is_base`, `total_special_seats_women`, `special_seats_women`)
VALUES
	(2, 4, 3, 3, 4, '0', '0', '1', '1', '', '0', 0, '', 1, '2018-06-21 18:04:01', 1, NULL, NULL, 'Configuración base - Van', 1, 0, 0, '');

INSERT INTO `expense_concept` ( `name`, `description`, `status`, `created_at`, `created_by`, `updated_at`, `updated_by`)
VALUES
	('Cancelación renta', 'Cancelación de renta de vans', 1, '2018-06-21 18:04:01', 1, NULL, NULL);

INSERT INTO `expense_concept` ( `name`, `description`, `status`, `created_at`, `created_by`, `updated_at`, `updated_by`)
VALUES
	('Devolución por liquidación de renta de van', 'Devolución por liquidación de renta de van', 1, '2018-06-21 18:04:01', 1, NULL, NULL);

INSERT INTO expense_concept (name, description, created_by) VALUES ('Cancelación renta', 'Concepto de cancelación de una renta de van', 1);

INSERT INTO `casefile` (name, description, status, created_at, created_by, updated_at, updated_by, type_casefile)
VALUES ('Copia IFE',NULL,1,'2018-06-26 23:49:30',1,NULL,NULL,NULL),
('Copia CURP',NULL,1,'2018-06-26 23:49:43',1,NULL,NULL,NULL),
('Copia Comprobante de Domicilio',NULL,1,'2018-06-26 23:51:32',1,NULL,NULL,NULL),
('Copia Acta de Nacimiento',NULL,1,'2018-06-26 23:51:58',1,NULL,NULL,NULL),
('Constancia de estudios',NULL,1,'2018-06-26 23:52:12',1,NULL,NULL,NULL),
('Número de Seguro Social',NULL,1,'2018-06-26 23:52:36',1,NULL,NULL,NULL),
('UMF',NULL,1,'2018-06-26 23:52:51',1,NULL,NULL,NULL),
('Cuenta bancaria y banco (nómina)',NULL,1,'2018-06-26 23:53:53',1,NULL,NULL,NULL);

INSERT INTO `characteristic` (`id`,`name`,`status`,`created_at`,`created_by`,`updated_at`,`updated_by`,`icon`)
VALUES (1,'Wifi gratuito',1,'2018-08-04 01:03:51',1,NULL,NULL,'icon-wifi'),
(2,'Toma corrientes',1,'2018-08-04 01:06:26',1,NULL,NULL,'icon-battery-charging-30'),
(3,'Pantallas',1,'2018-08-04 01:10:27',1,NULL,NULL,'icon-monitor-multiple'),
(4,'Aire acondicionado',1,'2018-08-04 01:12:46',1,NULL,NULL,'icon-weather-windy'),
(5,'Carga USB',1,'2018-08-04 01:14:00',1,NULL,NULL,'icon-usb'),
(6,'GPS',1,'2018-08-04 01:14:54',1,NULL,NULL,'icon-crosshairs-gps'),
(7,'Automático',1,'2018-08-04 17:40:03',1,NULL,NULL,'icon-plus-network'),
(8,'Manual',1,'2018-08-04 17:40:26',1,NULL,NULL,'icon-sitemap');

INSERT INTO `complement` (`id`,`name`,`description`,`type_complement`,`travel_insurance`,`travel_insurance_cost`,`status`,`created_at`)
VALUES (2,'Maleta documentada',NULL,'piece',0,NULL,1,'2018-07-05 03:24:47'),
(3,'Carga refrigerada',NULL,'package',0,NULL,1,'2018-07-05 03:24:56'),
(4,'Viaja con tu mascota',NULL,'size',0,NULL,1,'2018-07-05 03:25:04');

INSERT INTO `complement_rule` (`id`,`complement_id`,`min_quantity`,`max_quantity`,`max_weight`,`max_linear_volume`,`parent_id`,`status`,`created_at`,`created_by`)
VALUES (1,2,1,1,35.00,160.00,NULL,1,'2018-09-25 11:50:30',1),
(2,2,2,2,50.00,160.00,1,1,'2018-09-25 11:50:30',1),
(3,2,3,9999,0.00,0.00,2,1,'2018-09-25 12:13:47',1);

INSERT INTO `currency` (`id` ,`name`,`abr`,`symbol`,`icon`,`status`,`created_at`,`created_by`)
VALUES (19,'Euro','EUR','€',NULL,1,'2018-07-24 02:30:06',1),
(20,'Dólar canadiense','CAD','C$',NULL,1,'2018-07-24 02:30:06',1),
(21,'Yen','JPY','¥',NULL,2,'2018-07-24 02:30:06',1),
(22,'Peso mexicano','MXN','$',NULL,1,'2018-07-24 02:30:06',1),
(23,'Dólar estadounidense','USD','$',NULL,1,'2018-07-24 02:30:06',1),
(24,'Libra esterlina','GBP','£',NULL,2,'2018-07-24 02:30:06',1);

INSERT INTO `currency_denomination` (`id` ,`currency_id`,`denomination`,`status`,`created_at`,`created_by`)
VALUES (2,22,0.50,1,'2018-08-29 18:04:54',1),
(3,22,1.00,1,'2018-08-29 18:05:00',1),
(4,22,2.00,1,'2018-08-29 18:05:06',1),
(5,22,3.00,3,'2018-08-29 18:05:09',1),
(6,22,5.00,1,'2018-08-29 18:06:31',1),
(7,22,10.00,1,'2018-08-29 18:06:35',1),
(8,22,20.00,1,'2018-08-29 18:06:40',1),
(9,22,50.00,1,'2018-08-29 18:06:44',1),
(10,22,100.00,1,'2018-08-29 18:06:49',1),
(11,22,200.00,1,'2018-08-29 18:06:52',1),
(12,22,500.00,1,'2018-08-29 18:06:56',1),
(13,22,1000.00,1,'2018-08-29 18:07:00',1);

INSERT INTO `general_setting` (`id` ,`FIELD` ,`value`,`type_field`,`required_field`,`description`,`value_default`,`label_text`,`explanation_text`,`order_field`,`type_setting`,`group_type`,`status`,`created_at`,`created_by`, `updated_at`,`updated_by`)
VALUES (1,'driver_van_id','39','select',1,'Puesto  para hacer cálculo de costo por chofer','','Puesto de chofer de van','jobs?query=*,status=1',8,'0','rental',1,'2018-08-28 23:31:29',11,'2018-10-22 15:57:28',11),
(2,'van_earnings','10','percent',1,'Margen de ganancia por la renta de chofer en van','','Margen de ganancia por chofer','',9,'0','rental',1,'2018-08-28 23:32:23',11,'2018-09-19 00:38:42',1),
(5,'rental_minamount_percent','30','percent',1,'Porcentaje mínimo a cubrir para la renta de vans','30','% de anticipo para renta de van','',3,'0','rental',1,'2018-09-07 22:34:30',11,'2018-09-18 23:34:35',1),
(6,'penalties_hours','24','number',1,'Penalización por dia ...','','Horas de penalización','...',1,'0','rental',1,'2018-09-15 18:30:51',1,'2018-10-22 15:57:59',11),
(7,'penalties_percent','100','percent',1,'Cada {{penalties_extra_hours}}  hrs. se cobra ...','','Porcentaje de penalización','...',2,'0','rental',1,'2018-09-15 18:33:16',1,'2018-09-21 18:35:23',1),
(9,'time_before_cancel_van','24','number',1,'Tiempo previo con el que cuenta un cliente para  cancelar la renta sin perdida total del importe pagado','','Tiempo de cancelación de  renta de van','.',5,'2','rental',1,'2018-09-15 18:35:09',1,'2018-09-19 00:38:09',1),
(10,'cancel_penalty_percent','30','percent',1,'Porcentaje de penalización en la cancelación de renta de van','30','% de penalización','',6,'2','rental',1,'2018-09-15 18:36:16',1,'2018-09-19 00:38:02',1),
(11,'quotation_expired_after','15','number',1,'Días de vigencia de cotización de renta de vans','15','Días de vigencia de cotización','',7,'1','rental',1,'2018-09-15 18:37:14',1,'2018-09-23 23:18:02',11),
(12,'expire_open_tickets_after','0','number',0,'Días que tiene para  hacer efectivo su regreso  despues de la fecha de salida','0','Días límite para hacer efectivo el ticket abierto','',1,'2','travel',1,'2018-09-15 18:38:24',1,'2018-10-22 15:58:42',11),
(13,'time_before_checkin','2','number',1,'Horas previas a la salida del autobus en las cuales el cliente puede realizar checkin (documentar)','2','Horas antes del viaje para documentar','',2,'1','travel',1,'2018-09-15 18:40:09',1,'2018-10-22 15:58:51',11),
(14,'travel_package_discount','30','percent',0,'Porcentaje de descuento en paquetería, si el pasajero viaja con el paquete','','% descto. viaja con tu paquete','',3,'0','travel',1,'2018-09-15 18:42:39',1,'2018-10-22 15:59:05',11),
(15,'tickets_footer','¡¡MUCHAS GRACIAS POR SU COMPRA!! ','text',1,'Información mostrada en el pie de página del  comprobnante de pago','','Pie de página de los comprobantes de pago','',6,'0','tickets',1,'2018-09-15 18:46:55',1,'2018-10-22 15:44:52',11),
(16,'tickets_header','Autotransportes y Carga All Abordo S.A.P.I de C.V.','text',1,'Información mostrada en los encabezados de los comprobantes de pago','','Encabezado de comprobantes de pago','',1,'0','tickets',1,'2018-09-24 19:29:57',1,'2018-10-22 21:27:09',11),
(17,'tickets_rfc','AFA170911HY7','varchar',1,'RFC de las oficinas de Abordo','','RFC','',2,'0','tickets',3,'2018-09-24 22:54:32',1,NULL,NULL),
(18,'tickets_address','Zaragoza 240 nte, CP: 81200','varchar',1,'Dirección de la oficina matriz de Abordo','','Address','',3,'0','tickets',3,'2018-09-24 22:55:47',1,NULL,NULL),
(19,'tickets_city','Los Mochis, Sinaloa','varchar',1,'Ciudad donde se ubica la oficina matriz de Abordo','','City','',4,'0','tickets',3,'2018-09-24 22:56:48',1,NULL,NULL),
(20,'tickets_tax_regime','REGIMEN GENERAL DE LEY DE PERSONAS MORALES','varchar',1,'Regímen fiscal','','Tax regime','',5,'0','tickets',3,'2018-09-24 22:59:21',1,NULL,NULL),
(21,'kids_type_passanger','2','select',1,'Id de el tipo de viajero de menores',NULL,'Tipo de viajero para niños','specialTickets?query=*,status=1',NULL,'0','travel',1,'2018-09-26 20:40:20',11,'2018-10-22 15:51:31',11),
(22,'under_age','3','number',1,'Años de edad hasta los que no se cobra un boleto',NULL,'Edad a  partir de la que hay cobro por boleto',NULL,NULL,'0','travel',1,'2018-09-26 20:41:15',11,'2018-10-22 15:52:04',11),
(24,'iva','16','percent',1,'Porcentaje de impuesto al valor agregado',NULL,'IVA %',NULL,NULL,'0','general',1,'2018-09-29 18:12:13',1,'2018-10-22 15:57:13',11),
(29,'currency_id','22','select',1,'Divisa base',NULL,'Divisa base MXN','currencies?query=*,status=1',NULL,'0','general',1,'2018-10-09 22:44:18',1,'2018-10-10 16:32:17',11),
(30,'mi campo','mi valor','mi tipo',1,'mi descripcion','def','label','texto de explicacion',6,'2',NULL,3,'2018-10-10 00:33:39',1,NULL,NULL),
(31,'time_before_cancel_travel','4','number',1,'Tiempo previo con el que cuenta un cliente para cancelar un boleto de viaje sin pérdida total del importe pagado',NULL,'Tiempo de cancelación de pasaje de autobús',NULL,4,'0','travel',1,'2018-10-13 17:46:45',11,'2018-10-13 18:12:16',11),
(32,'cancel_penalty_percent_travel','10','percent',1,'Porcentaje de penalización en la cancelación de pasaje de autobús','30','% de penalización en cancelaciones.',NULL,5,'0','travel',1,'2018-10-13 18:04:00',11,NULL,NULL),
(33,'changes_permited_on_travel','3','number',1,'Límite de cambios permitidos en una reservación','3','Número límite de cambios permitidos',NULL,6,'0','travel',1,'2018-10-13 18:05:41',11,NULL,NULL),
(34,'time_before_change_personal_travel','2','number',1,'Tiempo previo con el que cuenta un cliente para  realizar cambio en información personal',NULL,'Horas de anticipación para realizar cambios de inf. del pasajero.',NULL,7,'0','travel',1,'2018-10-13 18:06:35',11,NULL,NULL),
(35,'change_personal_penalty_percent_travel','0','percent',1,'Porcentaje de penalización en cambios de información del pasajero en autobús',NULL,'% de penalización por cambios de inf. del pasajero',NULL,8,'0','travel',1,'2018-10-13 18:07:42',11,'2018-10-22 16:01:03',11),
(36,'time_before_change_route','4','number',1,'Tiempo previo con el que cuenta un cliente para  realizar cambio en su itinerario de viaje.',NULL,'Horas de anticipación para realizar cambios en viaje.',NULL,9,'0','travel',1,'2018-10-13 18:09:43',11,NULL,NULL),
(37,'change_route_penalty_percent_travel','0','percent',1,'Porcentaje de penalización en cambios de  ruta u horarios en autobuses.','0','% penalización por cambios de ruta o fechas.',NULL,10,'0','travel',1,'2018-10-13 18:10:54',11,NULL,NULL),
(38,'damage_charge_id','4','select',1,'Cargo extra sobre daños a complementos',NULL,'Concepto por daño a complemento','extraCharges?query=*,status=1,on_reception=1',NULL,'0','rental',1,'2018-10-18 18:03:24',11,'2018-10-18 18:25:51',11),
(39,'mi campo','mi valor','mi tipo',1,'mi descripcion','def','label','texto de explicacion',6,'2',NULL,1,'2018-10-22 15:21:49',19,NULL,NULL);

INSERT INTO `package_price` (id, name_price, min_linear_volume, max_linear_volume, min_weight, max_weight, price, currency_id, shipping_type)
VALUES (1,'T0',0.00,0.09,0.00,0.09,0.00,22,'parcel'),
(2,'T1',0.10,10.99,0.10,32.99,0.10,22,'parcel'),
(3,'T2',11.00,20.99,33.00,64.99,0.30,22,'parcel'),
(4,'T3',21.00,30.99,65.00,96.99,0.60,22,'parcel'),
(5,'T4',31.00,40.99,97.00,128.99,0.90,22,'parcel'),
(6,'T5',41.00,9999.99,129.00,9999.99,1.00,22,'parcel');
INSERT INTO package_price (name_price, min_linear_volume, max_linear_volume, min_weight, max_weight, price, currency_id, shipping_type)
VALUES ('TS', 0.0, 0.0, 0.0, 0.0, 0.70, 22, 'courier');


INSERT INTO `package_price_km` (`id` ,`min_km`,`max_km`,`price` ,`currency_id`,`parent_id`,`shipping_type`,`status`,`created_at`)
VALUES (1,0.00,100.99,100.00,22,NULL,'parcel',1,'2018-09-22 13:10:49'),
(2,201.00,500.99,200.00,22,1,'parcel',1,'2018-09-22 13:10:49'),
(3,501.00,1000.99,300.00,22,2,'parcel',1,'2018-09-22 13:10:49'),
(4,1001.00,1500.99,350.00,22,3,'parcel',1,'2018-09-22 13:10:49'),
(5,1501.00,9999.99,400.00,22,4,'parcel',1,'2018-09-22 13:10:49');

INSERT INTO `payment_method` (`id` ,`name`,`is_cash`,`status`,`icon`,`created_at`,`created_by`)
VALUES (1,'Efectivo',1,1,'icon-coin','2018-06-14 23:07:52',1),
(2,'Tarjeta (crédito/débito)',0,1,'icon-credit-card','2018-06-14 23:09:00',1);

INSERT INTO `paysheet_type` (`id` ,`name`,`type_payment`,`status`,`created_at`,`created_by`)
VALUES (1,'Pago semanal','weekend',1,'2018-06-12 17:45:59',1),
(2,'Quincenal','biweekly_pay',1,'2018-06-12 17:50:29',1);

INSERT INTO `requirement` (`id` ,`is_group`,`is_recurrent`,`parent_id`,`name`,`description`,`is_required`,`type_req`,`type_values`,`order_req`,`recurrent_type`,`status`,`created_at`,`created_by`)
VALUES (61,1,1,NULL,'Información de la salud','Información y archivos de la salud de los colaboradores',0,'','180',NULL,NULL,0,'2018-06-09 18:25:20',1),
(64,0,0,NULL,'Copia de cédula profesional','Comprobante de estudios',1,'file','',NULL,NULL,1,'2018-06-09 19:29:59',1),
(72,1,1,NULL,'driver_licence','Licencia que comprueba que el chofer tiene permitido conducir',1,'string','2018|2019|2020',NULL,'date',1,'2018-06-25 16:59:52',1);

INSERT INTO `special_ticket` (`id`, `name`, `description`,`has_discount`,`total_discount`,`status`,`created_at`,`created_by`,`updated_at`,`updated_by`,`base`,`available_tickets`,`has_preferent_zone`,`older_than`,`younger_than`)
VALUES (1,'Adulto','Pasaje normal',0,99,1,'2018-05-13 20:55:46',1,'2018-06-16 20:51:14',1,1,-1,0,12,120);

INSERT INTO `type_service` (`id` ,`name`,`description`,`controller` ,`status`,`created_at`,`created_by`,`updated_at`,`updated_by`)

VALUES (1,'Pasaje','Servicio de venta de boletos','travels',1,'2018-07-11 00:15:56',1,'2018-07-27 15:54:03',1),
(2,'Abordo Tours','Servicio de renta de vans','rental',1,'2018-07-11 01:08:30',1,'2018-07-27 15:55:36',1),
(3,'Paqueteria y mensajería','Servicio de paqueteria y mensajeria','packages',1,'2018-07-11 01:08:57',1,'2018-07-27 15:55:42',1);

INSERT INTO `type_vehicle` (`id`,`name`,`description`,`work_type`,`status`,`created_at`,`created_by`)
VALUES (1,'Van','Vehiculo del tipo van de trabajo','0',1,'2018-06-21 16:33:55',1),
(2,'Autobus','Vehiculo del tipo autobus de trabajo','1',1,'2018-06-21 16:34:55',1),
(3,'Vehiculo general','Vehiculo general para el uso del personal','1',1,'2018-06-21 16:37:13',1);