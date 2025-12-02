-- 1568325046 UP add-id-driver-in-general-setting-table
INSERT INTO general_setting (FIELD, value, type_field, required_field, description, label_text, explanation_text, type_setting, group_type)
VALUES ("driver_bus_id", 1, "select", 1, "Puesto para asignar chofer a un autobus", "Puesto de chofer de autobus", "jobs?query=*,status=1", 0, "travel");