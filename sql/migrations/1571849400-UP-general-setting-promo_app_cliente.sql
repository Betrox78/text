-- 1571849400 UP general-setting-promo_app_cliente
INSERT INTO general_setting(FIELD, value, type_field, required_field, description, value_default, label_text, explanation_text, order_field, type_setting, group_type)
VALUES('promo_app_cliente', '1', 'select', 1,
'Promoción general de aplicación cliente', '1',
'Promoción general de aplicación cliente',
'promos?query=*,status=1,purchase_origin=4', 0, '0', 'general');