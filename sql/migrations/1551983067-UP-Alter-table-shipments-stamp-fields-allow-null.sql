-- 1551983067 UP migrate
ALTER TABLE shipments 
CHANGE COLUMN left_stamp left_stamp VARCHAR(100) NULL ,
CHANGE COLUMN right_stamp right_stamp VARCHAR(100) NULL ;