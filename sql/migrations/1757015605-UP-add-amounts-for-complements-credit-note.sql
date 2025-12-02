-- 1757015605 UP add-amounts-for-complements-credit-note
ALTER TABLE `credit_note_detail`
ADD COLUMN `prev_available_amount_for_complement` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `total_amount`,
ADD COLUMN `new_available_amount_for_complement` DECIMAL(12,2) NULL DEFAULT '0.00' AFTER `prev_available_amount_for_complement`;