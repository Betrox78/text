-- 1647389067 UP new-column-timezone-to-branchoffice
alter table branchoffice
add column timezone varchar(30);

update branchoffice set timezone = "America/Mazatlan" where id = 1;
update branchoffice set timezone = "America/Mazatlan" where id = 2;
update branchoffice set timezone = "America/Mazatlan" where id = 3;
update branchoffice set timezone = "America/Mazatlan" where id = 4;
update branchoffice set timezone = "America/Mazatlan" where id = 5;
update branchoffice set timezone = "America/Mazatlan" where id = 6;
update branchoffice set timezone = "America/Mazatlan" where id = 7;
update branchoffice set timezone = "America/Mazatlan" where id = 8;
update branchoffice set timezone = "America/Mazatlan" where id = 9;
update branchoffice set timezone = "America/Mazatlan" where id = 10;
update branchoffice set timezone = "America/Tijuana" where id = 11;