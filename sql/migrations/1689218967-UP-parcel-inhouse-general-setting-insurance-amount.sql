-- 1689218967 UP parcel inhouse general setting insurance amount
INSERT INTO general_setting(id, FIELD, value, type_field, required_field, description, value_default, label_text, group_type, created_by) VALUES
('87', 'insurance_percent_inhouse', '1.', 'percent', '1',
'Porcentaje del seguro In-House, se aplica el porcentaje al valor declarado (insurance_value) para obtener el costo del seguro (insurance_amount)',
'.008', 'Porcentaje de seguro In-House', 'parcel', '1');