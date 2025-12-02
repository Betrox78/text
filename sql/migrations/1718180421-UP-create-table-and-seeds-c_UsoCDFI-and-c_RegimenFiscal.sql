-- 1718180421 UP create table and seeds c_UsoCDFI and c_RegimenFiscal
CREATE TABLE c_UsoCFDI(
	id INTEGER AUTO_INCREMENT PRIMARY KEY,
    c_UsoCFDI VARCHAR(5) NOT NULL,
    description VARCHAR(255) NOT NULL,
    apply_fisica BOOLEAN DEFAULT TRUE,
    apply_moral BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    created_by INTEGER NOT NULL,
    updated_at TIMESTAMP DEFAULT NULL,
    updated_by INTEGER DEFAULT NULL,
    CONSTRAINT fk_c_UsoCFDI_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_c_UsoCFDI_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE NO ACTION
);

INSERT INTO c_UsoCFDI(c_UsoCFDI, description, apply_fisica, apply_moral, created_by)
VALUES
("G01", "Adquisición de mercancías.", TRUE, TRUE, 1),
("G02", "Devoluciones, descuentos o bonificaciones.", TRUE, TRUE, 1),
("G03", "Gastos en general.", TRUE, TRUE, 1),
("I01", "Construcciones.", TRUE, TRUE, 1),
("I02", "Mobiliario y equipo de oficina por inversiones.", TRUE, TRUE, 1),
("I03", "Equipo de transporte.", TRUE, TRUE, 1),
("I04", "Equipo de computo y accesorios.", TRUE, TRUE, 1),
("I05", "Dados, troqueles, moldes, matrices y herramental.", TRUE, TRUE, 1),
("I06", "Comunicaciones telefónicas.", TRUE, TRUE, 1),
("I07", "Comunicaciones satelitales.", TRUE, TRUE, 1),
("I08", "Otra maquinaria y equipo.", TRUE, TRUE, 1),
("D01", "Honorarios médicos, dentales y gastos hospitalarios.", TRUE, FALSE, 1),
("D02", "Gastos médicos por incapacidad o discapacidad.", TRUE, FALSE, 1),
("D03", "Gastos funerales.", TRUE, FALSE, 1),
("D04", "Donativos.", TRUE, FALSE, 1),
("D05", "Intereses reales efectivamente pagados por créditos hipotecarios (casa habitación).", TRUE, FALSE, 1),
("D06", "Aportaciones voluntarias al SAR.", TRUE, FALSE, 1),
("D07", "Primas por seguros de gastos médicos.", TRUE, FALSE, 1),
("D08", "Gastos de transportación escolar obligatoria.", TRUE, FALSE, 1),
("D09", "Depósitos en cuentas para el ahorro, primas que tengan como base planes de pensiones.", TRUE, FALSE, 1),
("D10", "Pagos por servicios educativos (colegiaturas).", TRUE, FALSE, 1),
("S01", "Sin efectos fiscales.  ", TRUE, TRUE, 1),
("CP01", "Pagos", TRUE, TRUE, 1),
("CN01", "Nómina", TRUE, FALSE, 1);

CREATE TABLE c_RegimenFiscal(
	id INTEGER AUTO_INCREMENT PRIMARY KEY,
    c_RegimenFiscal INTEGER NOT NULL,
    description VARCHAR(255) NOT NULL,
    apply_fisica BOOLEAN DEFAULT TRUE,
    apply_moral BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    created_by INTEGER NOT NULL,
    updated_at TIMESTAMP DEFAULT NULL,
    updated_by INTEGER DEFAULT NULL,
    CONSTRAINT fk_c_RegimenFiscal_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_c_RegimenFiscal_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE NO ACTION
);

INSERT INTO c_RegimenFiscal(c_RegimenFiscal, description, apply_fisica, apply_moral, created_by)
VALUES
(601, "General de Ley Personas Morales", FALSE, TRUE, 1),
(603, "Personas Morales con Fines no Lucrativos", FALSE, TRUE, 1),
(605, "Sueldos y Salarios e Ingresos Asimilados a Salarios", TRUE, FALSE, 1),
(606, "Arrendamiento", TRUE, FALSE, 1),
(607, "Régimen de Enajenación o Adquisición de Bienes", TRUE, FALSE, 1),
(608, "Demás ingresos", TRUE, FALSE, 1),
(610, "Residentes en el Extranjero sin Establecimiento Permanente en México", TRUE, TRUE, 1),
(611, "Ingresos por Dividendos (socios y accionistas)", TRUE, FALSE, 1),
(612, "Personas Físicas con Actividades Empresariales y Profesionales", TRUE, FALSE, 1),
(614, "Ingresos por intereses", TRUE, FALSE, 1),
(615, "Régimen de los ingresos por obtención de premios", TRUE, FALSE, 1),
(616, "Sin obligaciones fiscales", TRUE, FALSE, 1),
(620, "Sociedades Cooperativas de Producción que optan por diferir sus ingresos", FALSE, TRUE, 1),
(621, "Incorporación Fiscal", TRUE, FALSE, 1),
(622, "Actividades Agrícolas, Ganaderas, Silvícolas y Pesqueras", FALSE, TRUE, 1),
(623, "Opcional para Grupos de Sociedades", FALSE, TRUE, 1),
(624, "Coordinados", FALSE, TRUE, 1),
(625, "Régimen de las Actividades Empresariales con ingresos a través de Plataformas Tecnológicas", TRUE, FALSE, 1),
(626, "Régimen Simplificado de Confianza", TRUE, TRUE, 1);