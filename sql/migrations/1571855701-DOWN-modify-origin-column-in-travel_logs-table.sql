-- 1571855701 DOWN modify origin column in travel_logs table
ALTER TABLE travel_logs
MODIFY COLUMN origin ENUM('app-operation','operation','web', 'app') DEFAULT 'app';

UPDATE travel_logs SET origin = 'app-operation' WHERE origin = 'app';
UPDATE travel_logs SET origin = 'operation' WHERE origin = 'web';

ALTER TABLE travel_logs
MODIFY COLUMN origin ENUM('app-operation', 'operation') DEFAULT 'app-operation';