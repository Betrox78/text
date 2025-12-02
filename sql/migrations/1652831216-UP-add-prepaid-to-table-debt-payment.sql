-- 1652831216 UP add-prepaid-to-table-debt-payment
alter table debt_payment
add column parcel_prepaid_id int(11) DEFAULT NULL;
