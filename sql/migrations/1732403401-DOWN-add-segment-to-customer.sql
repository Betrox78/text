-- 1732403401 DOWN add-segment-to-customer
ALTER TABLE customer
ADD COLUMN segment_id int(11) DEFAULT NULL,
ADD COLUMN notes_invoice VARCHAR(255) DEFAULT NULL AFTER notes;

ALTER TABLE customer
ADD CONSTRAINT `fk_customer_segment_id`
FOREIGN KEY (segment_id) REFERENCES `segment` (`id`);