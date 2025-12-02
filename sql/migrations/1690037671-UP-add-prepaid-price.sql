-- 1690037671 UP add-prepaid-price
alter table pp_price
add column min_m3 decimal(6,3),
add column max_m3 decimal(6,3);