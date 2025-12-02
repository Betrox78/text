-- 1569006271 UP parcel delivery target in general settings was added
INSERT INTO general_setting (FIELD, value, type_field, description, value_default, label_text, type_setting, group_type)
VALUES ('parcel_delivery_target', 93, 'percent', 'Objetivo de entrega de paquetería', 93, 'Objetivo de entrega de paquetería', '0', 'parcel');