-- 1678155202 DOWN add-column-prepaid-id
ALTER TABLE boarding_pass
DROP COLUMN prepaid_id;
DROP INDEX `boarding_pass_prepaid_id_idx`;
