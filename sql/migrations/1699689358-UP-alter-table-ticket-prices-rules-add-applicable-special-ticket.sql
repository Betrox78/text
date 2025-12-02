-- 1699689358 UP alter table ticket prices rules add applicable special ticket
ALTER TABLE ticket_prices_rules
ADD COLUMN applicable_special_ticket VARCHAR(50) DEFAULT NULL AFTER type_travel;