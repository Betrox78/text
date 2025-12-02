-- 1547057574 UP add-column-wish-pet-travel-in-boarding-pass
ALTER TABLE boarding_pass
ADD COLUMN wish_pet_travel tinyint(1) DEFAULT 0 NOT NULL;