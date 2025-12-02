-- 1757015605 DOWN add-amounts-for-complements-credit-note
ALTER TABLE `credit_note_detail`
  DROP COLUMN `prev_available_amount_for_complement`,
  DROP COLUMN `new_available_amount_for_complement`;