-- 1553720890 UP set null user id employee when delete user
DROP TRIGGER IF EXISTS setNullUserIdEmployeeWhenDeleteUser;
DELIMITER $$

CREATE DEFINER = CURRENT_USER TRIGGER setNullUserIdEmployeeWhenDeleteUser AFTER UPDATE ON users

FOR EACH ROW
BEGIN

IF (NEW.status = 3) THEN
UPDATE employee SET
user_id = null,
updated_by = NEW.updated_by,
updated_at = NOW()
WHERE
user_id = NEW.id;
END IF;
  END $$

DELIMITER ;