-- 1650559138 UP new-column-iva-to-branchoffice
alter table branchoffice
add column iva double default 0.16;