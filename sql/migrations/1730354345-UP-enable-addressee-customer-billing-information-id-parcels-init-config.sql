-- 1730354345 UP enable addressee customer billing information id parcels init config
ALTER TABLE parcels_init_config
ADD COLUMN enable_column_price_package
	BOOLEAN DEFAULT TRUE NOT NULL AFTER enable_addressee_customer_billing_information_id;