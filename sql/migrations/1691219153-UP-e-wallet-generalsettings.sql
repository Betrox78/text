-- 1691219153 UP e-wallet-generalsettings
INSERT INTO general_setting(FIELD, value, type_field, required_field, description, value_default, label_text, group_type, created_by)
VALUES ('parcel_e_wallet_percent', '1', 'percent', '1', 'Porcentaje a bonificar a monedero electrónico al realizar un envío de paquetería', '1', 'Porcentaje de bonificación paquetería', 'e-wallet', '1');

INSERT INTO general_setting(FIELD, value, type_field, required_field, description, value_default, label_text, group_type, created_by)
VALUES ('bonus_reference_e_wallet_percent', '1', 'percent', '1', 'Porcentaje a bonificar a monedero electrónico por referido', '1', 'Porcentaje de bonificación por referido', 'e-wallet', '1');
