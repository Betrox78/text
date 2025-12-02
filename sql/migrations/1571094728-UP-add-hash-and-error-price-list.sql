-- 1571094728 UP add-hash-and-error-price-list
ALTER TABLE prices_lists
ADD COLUMN hash VARCHAR(100),
ADD COLUMN error VARCHAR(100),
ADD CONSTRAINT hashCo UNIQUE (hash);