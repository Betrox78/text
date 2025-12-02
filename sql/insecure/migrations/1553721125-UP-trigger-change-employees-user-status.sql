-- 1553721125 UP trigger change employees user status
DROP TRIGGER IF EXISTS changeEmployeesUserStatus;
DELIMITER $$

CREATE DEFINER = CURRENT_USER TRIGGER changeEmployeesUserStatus AFTER UPDATE ON employee
FOR EACH ROW
BEGIN

IF (OLD.status <> NEW.status AND OLD.user_id IS NOT NULL) THEN
UPDATE users SET
status = NEW.status,
updated_by = NEW.updated_by,
updated_at = NOW()
WHERE id = OLD.user_id;
END IF;
  END $$

DELIMITER ;