-- 1558989655 UP currency foreign key in packing table was fixed
ALTER TABLE packings
DROP FOREIGN KEY packings_fk_1,
DROP COLUMN currency_id;

ALTER TABLE packings
ADD COLUMN currency_id int(11) DEFAULT NULL AFTER cost,
ADD CONSTRAINT packings_fk_1 FOREIGN KEY(currency_id) REFERENCES currency(id) ON DELETE SET NULL ON UPDATE CASCADE;