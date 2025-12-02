-- 1555111188 UP procedure assign prefix cash registers
DROP PROCEDURE IF EXISTS `cash_registers_prefix_assign`;
DELIMITER $$
CREATE PROCEDURE `cash_registers_prefix_assign`()
BEGIN
	DECLARE quantity INT DEFAULT 0;
    DECLARE currentId INT DEFAULT 0;
DECLARE quantityCurrentBranch INT DEFAULT 0;
DECLARE i INT DEFAULT 0;
SET quantity = (SELECT COUNT(id) FROM cash_registers);
CR: LOOP
SET i = i + 1;
IF i <= quantity THEN
SET currentId = (SELECT id FROM cash_registers WHERE id = i);
SET quantityCurrentBranch =
(SELECT COUNT(id) FROM cash_registers WHERE branchoffice_id =
(SELECT branchoffice_id FROM cash_registers WHERE id = currentId) AND prefix IS NOT NULL);
UPDATE cash_registers SET prefix = CONCAT('C', quantityCurrentBranch+1) WHERE id = currentId;
ITERATE CR;
END IF;
LEAVE CR;
END LOOP CR;
END $$
DELIMITER ;

CALL `cash_registers_prefix_assign`;