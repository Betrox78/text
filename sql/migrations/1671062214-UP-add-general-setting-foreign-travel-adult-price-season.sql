-- 1671062214 UP add general setting foreign travel adult price season
INSERT INTO general_setting(FIELD, value, type_field, description, value_default, label_text, group_type)
 VALUES('foreign_travel_adult_price_season', '1,2022-12-15,2022-12-31', 'text',
	'Temporada de solo precios adulto en los viajes foraneos(1[activa]/0[desactiva], fecha inicio[aaaa-mm-dd], fecha fin[aaaa-mm-dd])',
	'1,2022-12-15,2022-12-31', 'Temporada de solo precios adulto en los viajes foraneos(1[activa]/0[desactiva], fecha inicio[aaaa-mm-dd], fecha fin[aaaa-mm-dd])', 'travel');