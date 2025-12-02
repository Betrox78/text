-- 1704299823 DOWN set update remolque c config autotransporte
UPDATE c_ConfigAutotransporte SET remolque = '0' WHERE clave IN ('C2R2', 'C3R2', 'C2R3', 'C3R3', 'T2S1', 'T2S2', 'T2S3', 'T3S1',
'T3S2', 'T3S3', 'T2S1R2', 'T2S2R2', 'T2S1R3', 'T3S1R2', 'T3S1R3', 'T3S2R2', 'T3S2R3', 'T3S2R4', 'T2S2S2', 'T3S2S2', 'T3S3S2');