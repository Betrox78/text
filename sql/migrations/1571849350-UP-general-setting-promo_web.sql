-- 1571849350 UP general-setting-promo_web
INSERT INTO general_setting(FIELD, value, type_field, required_field, description, value_default, label_text, explanation_text, order_field, type_setting, group_type)
VALUES('promo_web', '1', 'select', 1,
'Promoción general del sitio', '1',
'Promoción general del sitio',
'promos?query=*,status=1,purchase_origin=2', 0, '0', 'general');