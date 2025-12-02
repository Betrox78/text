-- 1704225762 UP c config autotransporte remolque column and new values
ALTER TABLE c_ConfigAutotransporte
ADD COLUMN remolque VARCHAR(30) DEFAULT '0' NOT NULL AFTER num_llantas;