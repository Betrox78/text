-- 1553646245 UP change signature to default null in parcels deliveries table
ALTER TABLE parcels_deliveries
MODIFY signature varchar(254) DEFAULT NULL AFTER no_credential;