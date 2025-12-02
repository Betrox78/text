-- 1543950679 UP create-table-payback
CREATE TABLE IF NOT EXISTS payback(
currency_id int(11) NOT NULL COMMENT 'Id del currency base que esté especificado en la transacción',
points decimal(12,2) NOT NULL DEFAULT 0 COMMENT 'Puntos equivalentes',
km int(11) NOT NULL DEFAULT 0 COMMENT 'Km equivalentes a puntos',
money decimal(12,2) NOT NULL DEFAULT 0.0 COMMENT 'Dinero equivalente por km y puntos',
service_type enum('boarding','rental','parcel') NOT NULL COMMENT '‘boarding’: Boarding pass, ‘rental’: Rental, ‘parcel’: Parcels',
created_by int(11) DEFAULT NULL,
created_at datetime DEFAULT NOW(),
updated_by int(11) DEFAULT NULL,
updated_at datetime DEFAULT NULL,
CONSTRAINT fk_payback_currency_id FOREIGN KEY (currency_id) REFERENCES currency(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO payback (currency_id, points, km, money, service_type) VALUES((SELECT value FROM general_setting WHERE FIELD='currency_id'), 1, 1, 0.01, 'boarding');
INSERT INTO payback (currency_id, points, km, money, service_type) VALUES((SELECT value FROM general_setting WHERE FIELD='currency_id'), 1, 1, 0.05, 'rental');
INSERT INTO payback (currency_id, points, km, money, service_type) VALUES((SELECT value FROM general_setting WHERE FIELD='currency_id'), 1, 1, 0.09, 'parcel');