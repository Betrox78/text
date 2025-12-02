-- 1546563819 UP add-image-field-to-alliance
ALTER TABLE alliance
ADD COLUMN img_file VARCHAR(255) NULL AFTER description;