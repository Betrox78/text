-- 1686267063 DOWN add permision and module parcel reception in house
DELETE FROM permission WHERE sub_module_id = 138;
DELETE FROM sub_module WHERE id = 138;
