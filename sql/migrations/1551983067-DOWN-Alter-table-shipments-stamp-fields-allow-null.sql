-- 1551983067 DOWN migrate
ALTER TABLE shipments 
CHANGE COLUMN left_stamp left_stamp VARCHAR(100) NOT NULL ,
CHANGE COLUMN right_stamp right_stamp VARCHAR(100) NOT NULL ;