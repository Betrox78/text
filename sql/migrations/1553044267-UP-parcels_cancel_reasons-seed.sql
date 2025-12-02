-- 1553044267 UP parcels_cancel_reasons seed
INSERT INTO parcels_cancel_reasons
(name, responsable, cancel_type, created_by)
VALUES
('Error de documentación', 'company', 'fast_cancel', 1),
('Error de documentación', 'customer', 'fast_cancel', 1),
('Daño a los paquetes', 'others', 'end_cancel', 1),
('Extravío de mercancía', 'others', 'end_cancel', 1),
('Decomiso de mercancía', 'others', 'end_cancel', 1);