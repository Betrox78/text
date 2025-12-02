-- 1635460727 UP modify-column-service-table-promos
ALTER TABLE promos
MODIFY COLUMN service enum('boardingpass','parcel','rental','guiapp') NOT NULL;