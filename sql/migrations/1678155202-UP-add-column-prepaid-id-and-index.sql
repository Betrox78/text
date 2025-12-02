-- 1678155202 UP add-column-prepaid-id

-- New column in boarding pass
ALTER TABLE boarding_pass
add column prepaid_id int(11) DEFAULT NULL;

-- CREATE INDEX OF PREPAID_ID
CREATE INDEX boarding_pass_prepaid_id_idx ON boarding_pass(prepaid_id);
