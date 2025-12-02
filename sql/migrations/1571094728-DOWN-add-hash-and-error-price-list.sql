-- 1571094728 DOWN add-hash-and-error-price-list
ALTER TABLE prices_lists
DROP COLUMN hash,
DROP COLUMN error;