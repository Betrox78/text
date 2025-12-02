-- 1553646245 DOWN change signature to default null in parcels deliveries table
ALTER TABLE parcels_deliveries
MODIFY signature varchar(254) NOT NULL AFTER no_credential;