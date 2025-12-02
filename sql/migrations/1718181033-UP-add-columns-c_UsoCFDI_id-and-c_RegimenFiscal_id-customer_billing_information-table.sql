-- 1718181033 UP add columns c_UsoCFDI_id and c_RegimenFiscal_id customer_billing_information table
ALTER TABLE customer_billing_information
ADD COLUMN c_UsoCFDI_id INTEGER DEFAULT NULL AFTER legal_person,
ADD COLUMN c_RegimenFiscal_id INTEGER DEFAULT NULL AFTER c_UsoCFDI_id,
ADD CONSTRAINT fk_customer_billing_information_c_UsoCFDI_id
	FOREIGN KEY (c_UsoCFDI_id) REFERENCES c_UsoCFDI(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
ADD CONSTRAINT fk_customer_billing_information_c_RegimenFiscal_id
	FOREIGN KEY (c_RegimenFiscal_id) REFERENCES c_RegimenFiscal(id) ON UPDATE NO ACTION ON DELETE NO ACTION;