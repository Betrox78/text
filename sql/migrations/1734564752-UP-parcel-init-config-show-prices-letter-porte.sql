-- 1734564752 UP parcel init config show prices letter porte
ALTER TABLE parcels_init_config
ADD COLUMN show_prices_in_letter_porte BOOLEAN NOT NULL DEFAULT TRUE AFTER enable_column_price_package;