-- 1702058992 UP name column trailers
ALTER TABLE trailers
ADD COLUMN name VARCHAR(30) NOT NULL AFTER id;