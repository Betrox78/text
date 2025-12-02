-- 1546638994 UP add-category-field-to-alliance
ALTER TABLE alliance
ADD COLUMN alliance_category_id INT NULL AFTER description;

ALTER TABLE alliance
ADD CONSTRAINT alliance_alliance_category_id
  FOREIGN KEY (alliance_category_id)
  REFERENCES alliance_category (id);