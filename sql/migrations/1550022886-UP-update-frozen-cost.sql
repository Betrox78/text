-- 1550022886 UP update frozen cost
DELETE FROM general_setting WHERE FIELD='frozen_cost';
INSERT INTO general_setting(FIELD, value, type_field, required_field, description, value_default, label_text, order_field, type_setting, group_type)
VALUES ('frozen_cost', '1.0','cureency', 0, 'Importe extra por el servicio de carga refrigerada', '1.0','Costo por carga refrigerada', 16, '0', 'frozen');