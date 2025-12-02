-- 1630708302 UP migrate_c_tipoEmbalaje_sat

ALTER TABLE `packings`
ADD COLUMN `clave_sat` VARCHAR(10) NULL AFTER `updated_by`,
ADD COLUMN `fecha_vigencia` DATETIME NULL AFTER `clave_sat`,
ADD COLUMN `fecha_vencimiento` DATETIME NULL AFTER `fecha_vigencia`;


ALTER TABLE `packings`
CHANGE COLUMN `name` `name` VARCHAR(150) NULL ,
CHANGE COLUMN `created_by` `created_by` INT(11) NULL ;

INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('1A1','Bidones (Tambores) de Acero 1 de tapa no desmontable','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('1A2','Bidones (Tambores) de Acero 1 de tapa desmontable','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('1B1','Bidones (Tambores) de Aluminio de tapa no desmontable','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('1B2','Bidones (Tambores) de Aluminio de tapa desmontable','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('1D','Bidones (Tambores) de Madera contrachapada','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('1G','Bidones (Tambores) de Cartón','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('1H1','Bidones (Tambores) de Plástico de tapa no desmontable','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('1H2','Bidones (Tambores) de Plástico de tapa desmontable','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('1N1','Bidones (Tambores) de Metal que no sea acero ni aluminio de tapa no desmontable','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('1N2','Bidones (Tambores) de Metal que no sea acero ni aluminio de tapa desmontable','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('3A1','Jerricanes (Porrones) de Acero de tapa no desmontable','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('3A2','Jerricanes (Porrones) de Acero de tapa desmontable','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('3B1','Jerricanes (Porrones) de Aluminio de tapa no desmontable','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('3B2','Jerricanes (Porrones) de Aluminio de tapa desmontable','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('3H1','Jerricanes (Porrones) de Plástico de tapa no desmontable','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('3H2','Jerricanes (Porrones) de Plástico de tapa desmontable','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('4A','Cajas de Acero','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('4B','Cajas de Aluminio','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('4C1','Cajas de Madera natural ordinaria','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('4C2','Cajas de Madera natural de paredes a prueba de polvos (estancas a los pulverulentos)','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('4D','Cajas de Madera contrachapada','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('4F','Cajas de Madera reconstituida','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('4G','Cajas de Cartón','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('4H1','Cajas de Plástico Expandido','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('4H2','Cajas de Plástico Rígido','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('5H1','Sacos (Bolsas) de Tejido de plástico sin forro ni revestimientos interiores','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('5H2','Sacos (Bolsas) de Tejido de plástico a prueba de polvos (estancos a los pulverulentos)','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('5H3','Sacos (Bolsas) de Tejido de plástico resistente al agua','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('5H4','Sacos (Bolsas) de Película de plástico','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('5L1','Sacos (Bolsas) de Tela sin forro ni revestimientos interiores','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('5L2','Sacos (Bolsas) de Tela a prueba de polvos (estancos a los pulverulentos)','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('5L3','Sacos (Bolsas) de Tela resistentes al agua','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('5M1','Sacos (Bolsas) de Papel de varias hojas','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('5M2','Sacos (Bolsas) de Papel de varias hojas, resistentes al agua','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6HA1','Envases y embalajes compuestos de Recipiente de plástico, con bidón (tambor) de acero','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6HA2','Envases y embalajes compuestos de Recipiente de plástico, con una jaula o caja de acero','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6HB1','Envases y embalajes compuestos de Recipiente de plástico, con un bidón (tambor) exterior de aluminio','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6HB2','Envases y embalajes compuestos de Recipiente de plástico, con una jaula o caja de aluminio','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6HC','Envases y embalajes compuestos de Recipiente de plástico, con una caja de madera','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6HD1','Envases y embalajes compuestos de Recipiente de plástico, con un bidón (tambor) de madera contrachapada','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6HD2','Envases y embalajes compuestos de Recipiente de plástico, con una caja de madera contrachapada','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6HG1','Envases y embalajes compuestos de Recipiente de plástico, con un bidón (tambor) de cartón','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6HG2','Envases y embalajes compuestos de Recipiente de plástico, con una caja de cartón','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6HH1','Envases y embalajes compuestos de Recipiente de plástico, con un bidón (tambor) de plástico','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6HH2','Envases y embalajes compuestos de Recipiente de plástico, con caja de plástico rígido','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6PA1','Envases y embalajes compuestos de Recipiente de vidrio, porcelana o de gres, con un bidón (tambor) de acero','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6PA2','Envases y embalajes compuestos de Recipiente de vidrio, porcelana o de gres, con una jaula o una caja de acero','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6PB1','Envases y embalajes compuestos de Recipiente de vidrio, porcelana o de gres, con un bidón (tambor) exterior de aluminio','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6PB2','Envases y embalajes compuestos de Recipiente de vidrio, porcelana o de gres, con una jaula o una caja de aluminio','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6PC','Envases y embalajes compuestos de Recipiente de vidrio, porcelana o de gres, con una caja de madera','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6PD 1','Envases y embalajes compuestos de Recipiente de vidrio, porcelana o de gres, con bidón (tambor) de madera contrachapada','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6PD2','Envases y embalajes compuestos de Recipiente de vidrio, porcelana o de gres, con canasta de mimbre','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6PG1','Envases y embalajes compuestos de Recipiente de vidrio, porcelana o de gres, con un bidón (tambor) de cartón','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6PG2','Envases y embalajes compuestos de Recipiente de vidrio, porcelana o de gres, con una caja de cartón','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6PH1','Envases y embalajes compuestos de Recipiente de vidrio, porcelana o de gres, con un envase y embalaje de plástico expandido','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('6PH2','Envases y embalajes compuestos de Recipiente de vidrio, porcelana o de gres, con un envase y embalaje de plástico rígido','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('7H1','Bultos de Plástico','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('7L1','Bultos de Tela','01/06/21',NULL);
INSERT INTO packings(clave_sat,description,fecha_vigencia,fecha_vencimiento) VALUES ('Z01','No aplica','01/06/21',NULL);



