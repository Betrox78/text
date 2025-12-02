-- 1552004240 UP add description column in permission table
ALTER TABLE permission
ADD COLUMN description varchar(255) NOT NULL DEFAULT '' AFTER name;