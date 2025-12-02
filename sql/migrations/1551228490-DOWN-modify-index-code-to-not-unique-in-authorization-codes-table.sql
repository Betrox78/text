-- 1551228490 DOWN modify-index-code-to-not-unique-in-authorization-codes-table
TRUNCATE authorization_codes;
ALTER TABLE authorization_codes
DROP INDEX authorization_codes_code_idx,
MODIFY COLUMN code varchar(50) NOT NULL;

CREATE UNIQUE INDEX code ON authorization_codes(code);