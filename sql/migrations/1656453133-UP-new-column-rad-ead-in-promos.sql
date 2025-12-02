-- 1656453133 UP new-column-rad-ead-in-promos
alter table promos
add column `apply_rad` tinyint(1) DEFAULT '0',
add column `apply_ead` tinyint(1) DEFAULT '0';