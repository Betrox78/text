-- 1576026690 UP add-observaciones-field-on-contpaqDocument
ALTER TABLE CONTPAQ_Documentos
    ADD COLUMN aObservaciones TEXT DEFAULT NULL;