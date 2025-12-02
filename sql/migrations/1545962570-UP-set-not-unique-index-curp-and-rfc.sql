-- 1545962570 UP set-not-unique-index-curp-and-rfc
ALTER TABLE employee
DROP INDEX rfc,
DROP INDEX curp;

CREATE INDEX rfc ON employee(rfc);
CREATE INDEX curp ON employee(curp);