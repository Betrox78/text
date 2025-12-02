-- 1568391930 UP create credit payments permission
INSERT INTO sub_module (id, name, module_id, group_type, created_by) VALUES
(75, 'app.credit_payments', 1, 'admin', 1),
(76, 'app.credit_payments', 2, 'billing', 1);

INSERT INTO permission (id, name, description, sub_module_id, created_by) VALUES
(176, '#create', 'Registrar pagos de venta a credito', 75, 1),
(177, '#create', 'Registrar pagos de venta a credito', 76, 1);