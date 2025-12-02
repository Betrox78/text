-- 1656607030 DOWN new-columns-pp-price-table
alter table pp_price
drop column dummy_height,
drop column dummy_length,
drop column dummy_width