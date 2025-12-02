-- 1574888413 UP add-error-message-field-on-cubic-log
ALTER TABLE cubic_log
    ADD COLUMN message MEDIUMTEXT NULL AFTER cubic_log_status;