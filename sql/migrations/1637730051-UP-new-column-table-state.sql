-- 1637730051 UP new-column-table-state
ALTER TABLE state
ADD COLUMN name_sat varchar(27) DEFAULT null;


update state set name_sat = "AGU" where name = "Aguascalientes";
update state set name_sat = "BCN" where name = "Baja California";
update state set name_sat = "BCS" where name = "Baja California Sur";
update state set name_sat = "CAM" where name = "Campeche";
update state set name_sat = "COA" where name = "Coahuila de Zaragoza";
update state set name_sat = "COL" where name = "Colima";

update state set name_sat = "CHH" where name = "Chihuahua";
update state set name_sat = "CHP" where name = "Chiapas";
update state set name_sat = "DUR" where name = "Durango";
update state set name_sat = "GRO" where name = "Guerrero";
update state set name_sat = "GUA" where name = "Guanajuato";

update state set name_sat = "HID" where name = "Hidalgo";
update state set name_sat = "JAL" where name = "Jalisco";
update state set name_sat = "MEX" where name = "México";
update state set name_sat = "MIC" where name = "Michoacán de Ocampo";
update state set name_sat = "MOR" where name = "Morelos";

update state set name_sat = "NAY" where name = "Nayarit";
update state set name_sat = "NLE" where name = "Nuevo León";
update state set name_sat = "OAX" where name = "Oaxaca";
update state set name_sat = "PUE" where name = "Puebla";
update state set name_sat = "QUE" where name = "Querétaro";

update state set name_sat = "ROO" where name = "Quintana Roo";
update state set name_sat = "SIN" where name = "Sinaloa";
update state set name_sat = "SLP" where name = "San Luis Potosí";
update state set name_sat = "SON" where name = "Sonora";
update state set name_sat = "TAB" where name = "Tabasco";

update state set name_sat = "TAM" where name = "Tamaulipas";
update state set name_sat = "TLA" where name = "Tlaxcala";
update state set name_sat = "VER" where name = "Veracruz de Ignacio de la Llave";
update state set name_sat = "YUC" where name = "Yucatán";
update state set name_sat = "ZAC" where name = "Zacatecas";
update state set name_sat = "DIF" where name = "Ciudad de México";