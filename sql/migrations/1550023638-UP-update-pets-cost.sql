-- 1550023638 UP update pets cost
DELETE FROM general_setting WHERE FIELD='pets_cost';
INSERT INTO general_setting(FIELD, value, type_field, required_field, description, value_default, label_text, order_field, type_setting, group_type)
VALUES ('pets_cost', '1.0','currency', 0, 'Costo de servicio de Mascotas', '1.0','Costo de servicio de Mascotas', 16, '0', 'pets');