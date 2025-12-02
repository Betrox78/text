-- 1549944797 UP Add baggage_cost in general settings
INSERT INTO general_setting (FIELD, value, type_field, required_field, description, value_default, label_text, type_setting, group_type, created_by) VALUES ('baggage_cost', '50', 'double', '1', 'Cargo extra por maleta', '0', 'Cargo extra por maleta en aplicación de chofer', '0', 'travel', '1');
