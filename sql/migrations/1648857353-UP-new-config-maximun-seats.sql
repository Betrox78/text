-- 1648857353 UP new-config-maximun-seats
insert into general_setting
(FIELD, value, type_field, required_field, description, value_default, label_text, order_field, group_type, created_by)
values ("max_seats_to_hide", 0, "number", 0, "Cantidad de asientos que se puedan vender en destinos intermedias de rutas hacia Tijuana", 0, "Cantidad de asientos que se puedan vender en destinos intermedias de rutas hacia Tijuana", 13, 'travel',1)