-- 1635460727 DOWN modify-column-service-table-promos
ALTER TABLE promos
MODIFY COLUMN service enum('boardingpass','parcel','rental') NOT NULL;