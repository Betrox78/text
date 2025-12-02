-- 1682053880 UP integration-partner-miticket
INSERT INTO integration_partner (name, created_by) 
VALUES ('miticket', (SELECT id FROM users WHERE email = 'admin@allabordo.com'))