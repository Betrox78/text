-- 1704236312 DOWN insert new values c config autotransporte
DELETE FROM c_ConfigAutotransporte WHERE clave IN ('VL', 'OTROEVGP', 'GPLUTA' ,'GPLUTB' ,'GPLUTC' ,'GPLUTD' ,'GPLATA', 'GPLATB' ,'GPLATC', 'GPLATD');

UPDATE c_ConfigAutotransporte SET status = 1 WHERE clave IN ('OTROEV', 'OTROEGP');