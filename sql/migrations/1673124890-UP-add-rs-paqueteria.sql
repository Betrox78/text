-- 1673124890 UP add-rs-paqueteria
alter table package_price_km
add column RS DECIMAL(12,2) default 0;

update package_price_km set RS=42 where id = 1;
update package_price_km set RS=91 where id = 2;
update package_price_km set RS=140 where id = 3;
update package_price_km set RS=162 where id = 4;
update package_price_km set RS=198 where id = 5;
update package_price_km set RS=234 where id = 7;
update package_price_km set RS=234 where id = 8;