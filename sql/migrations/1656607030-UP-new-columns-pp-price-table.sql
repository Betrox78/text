-- 1656607030 UP new-columns-pp-price-table
alter table pp_price
add column dummy_height decimal(12,2) default 0.0,
add column dummy_length decimal(12,2) default 0.0,
add column dummy_width decimal(12,2) default 0.0;

update pp_price set dummy_height = 1, dummy_length = 1, dummy_width = 1  where id = 1;
update pp_price set dummy_height = 10, dummy_length = 10, dummy_width = 10 where id = 2;
update pp_price set dummy_height = 20, dummy_length = 20, dummy_width = 20 where id = 3;
update pp_price set dummy_height = 30, dummy_length = 30, dummy_width = 30 where id = 4;
update pp_price set dummy_height = 40 , dummy_length = 40, dummy_width = 40 where id = 5;
update pp_price set dummy_height = 50, dummy_length = 50, dummy_width = 50 where id = 6;
update pp_price set dummy_height = 0 , dummy_length = 0, dummy_width = 0 where id = 7;
update pp_price set dummy_height = 60, dummy_length = 60, dummy_width = 60 where id = 8;
update pp_price set dummy_height = 70 , dummy_length = 70 , dummy_width = 70 where id = 9;