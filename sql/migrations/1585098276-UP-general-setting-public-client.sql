-- 1585098276 UP general-setting-public-client
SET @public_client := (SELECT id FROM customer where concat_ws(' ',first_name, last_name) = 'PUBLICO EN GENERAL');

INSERT INTO general_setting (FIELD, value, type_field, required_field, description, value_default, label_text, explanation_text
, type_setting, group_type, status, created_at)
VALUES ('public_client', @public_client, 'select', '0', 'Cliente abordo para uso de publico en general.', @public_client, 'Nombre de cliente para uso público en general', 'customers?query=*,status=1', '0', 'general', '1', NOW());
