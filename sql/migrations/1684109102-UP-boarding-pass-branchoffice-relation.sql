-- 1684109102 UP boarding-pass-branchoffice-relation

ALTER TABLE boarding_pass
ADD CONSTRAINT fk_boarding_pass_branchoffice
FOREIGN KEY (branchoffice_id) REFERENCES branchoffice(id);