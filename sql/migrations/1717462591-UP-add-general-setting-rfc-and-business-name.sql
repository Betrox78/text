-- 1717462591 UP add general setting rfc and business name
INSERT INTO general_setting(FIELD, value, type_field, description, value_default, label_text, type_setting, group_type)
VALUES ('rfc', 'ACA170911HY7', 'text', 'RFC de la empresa', 'ACA170911HY7', 'RFC', '0', 'general');

INSERT INTO general_setting(FIELD, value, type_field, description, value_default, label_text, type_setting, group_type)
VALUES ('business_name', 'AUTOTRANSPORTES Y CARGA PTX S.A.P.I. DE C.V.', 'text', 'Razón social de la empresa', 'AUTOTRANSPORTES Y CARGA PTX S.A.P.I. DE C.V.', 'Razón social', '0', 'general');