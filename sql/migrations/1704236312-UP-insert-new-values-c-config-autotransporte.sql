-- 1704236312 UP insert new values c config autotransporte
INSERT INTO c_ConfigAutotransporte
(clave, description, num_ejes, num_llantas, remolque, init_date, created_by)
VALUES
('VL', 'Vehículo ligero de carga (2 llantas en el eje delantero y 2 llantas en el eje trasero)', '2', '4', '0, 1', '2023-11-25 00:00:00', 1),
('OTROEVGP', 'Especializado de carga Voluminosa y/o Gran Peso', null, null, '0, 1', '2023-11-25 00:00:00', 1),
('GPLUTA', 'Grúa de Pluma Tipo A', null, null, '0', '2023-11-25 00:00:00', 1),
('GPLUTB', 'Grúa de Pluma Tipo B', null, null, '0', '2023-11-25 00:00:00', 1),
('GPLUTC', 'Grúa de Pluma Tipo C', null, null, '0', '2023-11-25 00:00:00', 1),
('GPLUTD', 'Grúa de Pluma Tipo D', null, null, '0', '2023-11-25 00:00:00', 1),
('GPLATA', 'Grúa de Plataforma Tipo A', null, null, '0', '2023-11-25 00:00:00', 1),
('GPLATB', 'Grúa de Plataforma Tipo B', null, null, '0', '2023-11-25 00:00:00', 1),
('GPLATC', 'Grúa de Plataforma Tipo C', null, null, '0', '2023-11-25 00:00:00', 1),
('GPLATD', 'Grúa de Plataforma Tipo D', null, null, '0', '2023-11-25 00:00:00', 1);

UPDATE c_ConfigAutotransporte SET status = 3 WHERE clave IN ('OTROEV', 'OTROEGP');