-- 1571855701 UP modify origin column in travel_logs table
ALTER TABLE travel_logs
MODIFY COLUMN origin ENUM('app-operation','operation','web', 'app') DEFAULT 'app';

UPDATE travel_logs SET origin = 'app' WHERE origin = 'app-operation';
UPDATE travel_logs SET origin = 'web' WHERE origin = 'operation';

ALTER TABLE travel_logs
MODIFY COLUMN origin ENUM('web', 'app') DEFAULT 'app';
