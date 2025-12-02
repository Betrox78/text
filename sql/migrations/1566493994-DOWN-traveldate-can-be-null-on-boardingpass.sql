-- 1566493994 DOWN traveldate-can-be-null-on-boardingpass
ALTER TABLE boarding_pass
MODIFY travel_date DATETIME NOT NULL;