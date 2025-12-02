-- 1756916282 DOWN add-total-amount-to-credit-note-tables
ALTER TABLE `credit_note`
  DROP COLUMN `total_amount`;

ALTER TABLE `credit_note_detail`
  DROP COLUMN `total_amount`;