-- 1545962570 DOWN set-not-unique-index-curp-and-rfc
ALTER TABLE employee
DROP INDEX rfc,
DROP INDEX curp;

CREATE UNIQUE INDEX rfc ON employee(rfc);
CREATE UNIQUE INDEX curp ON employee(curp);