-- 1551228490 UP modify-index-code-to-not-unique-in-authorization-codes-table
TRUNCATE authorization_codes;
ALTER TABLE authorization_codes
DROP INDEX code,
MODIFY COLUMN code varchar(6) NOT NULL;

CREATE INDEX authorization_codes_code_idx ON authorization_codes(code);