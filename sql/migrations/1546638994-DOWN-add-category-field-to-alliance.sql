-- 1546638994 DOWN add-category-field-to-alliance
ALTER TABLE alliance
DROP FOREIGN KEY alliance_alliance_category_id;

ALTER TABLE alliance
DROP COLUMN alliance_category_id;