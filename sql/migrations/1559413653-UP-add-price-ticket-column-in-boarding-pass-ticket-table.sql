-- 1559413653 UP add price ticket column in boarding pass ticket table
ALTER TABLE boarding_pass_ticket
ADD COLUMN price_ticket decimal(12,2) NOT NULL DEFAULT 0.00 AFTER total_amount;

UPDATE boarding_pass_ticket bpt SET bpt.price_ticket = (SELECT ctp.total_amount FROM config_ticket_price ctp WHERE ctp.id = bpt.config_ticket_price_id);