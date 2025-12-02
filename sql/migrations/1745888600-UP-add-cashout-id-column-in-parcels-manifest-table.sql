-- 1745888600 UP add cashout id column in parcels manifest table
ALTER TABLE parcels_manifest
ADD COLUMN cash_out_id INTEGER DEFAULT NULL AFTER id_branchoffice,
ADD CONSTRAINT fk_parcels_manifest_cash_out_id
	FOREIGN KEY (cash_out_id) REFERENCES cash_out(id) ON UPDATE NO ACTION ON DELETE NO ACTION;