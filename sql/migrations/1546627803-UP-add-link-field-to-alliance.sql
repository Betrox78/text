-- 1546627803 UP add-link-field-to-alliance
ALTER TABLE alliance
ADD COLUMN link VARCHAR(255) NULL AFTER description;