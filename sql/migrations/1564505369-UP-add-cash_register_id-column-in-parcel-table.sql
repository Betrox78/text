-- 1564505369 UP add cash_register_id column in parcel table
ALTER TABLE parcels
ADD COLUMN cash_register_id int(11) DEFAULT NULL AFTER exchange_rate_id,
ADD CONSTRAINT parcels_cash_register_id_fk FOREIGN KEY (cash_register_id) REFERENCES cash_registers(id);