-- 1694495972 UP add-bank-account-settings
INSERT INTO general_setting(FIELD, value, type_field, required_field, description, value_default, label_text, group_type, created_by) VALUES
('boarding_pass_bank_acc', '', 'text', '1', 'Numero de cuenta de banco a la cual se debe depositar el efectivo proveniente de pasaje', '', 'No. de cuenta para depositar pasaje', 'travel', '1');

INSERT INTO general_setting(FIELD, value, type_field, required_field, description, value_default, label_text, group_type, created_by) VALUES
('boarding_pass_bank', '', 'text', '1', 'Nombre del Banco al cual se debe depositar el efectivo proveniente de pasaje', '', 'Banco para deposito de pasaje', 'travel', '1');

INSERT INTO general_setting(FIELD, value, type_field, required_field, description, value_default, label_text, group_type, created_by) VALUES
('parcel_bank_acc', '', 'text', '1', 'Numero de cuenta de banco a la cual se debe depositar el efectivo proveniente de paqueteria', '', 'No. de cuenta para depositar paqueteria', 'parcel', '1');

INSERT INTO general_setting(FIELD, value, type_field, required_field, description, value_default, label_text, group_type, created_by) VALUES
('parcel_bank', '', 'text', '1', 'Nombre del Banco al cual se debe depositar el efectivo proveniente de paqueteria', '', 'Banco para deposito de paqueteria', 'parcel', '1');
