-- 1635179734 UP create_tables_guiapp_pacels_prepaid

create table IF NOT EXISTS parcels_prepaid(
id int(11) primary key unique not null auto_increment,
tracking_code varchar(20) not null unique,
shipment_type enum('OCU','RAD','EAD','RAD-EAD'),
payment_condition enum('credit','cash') not null,
purchase_origin enum('sucursal','web','kiosko','app cliente','app chofer'),
customer_id int (11),
crated_by int(11),
created_at datetime default current_timestamp,
updated_by int(11),
updated_at datetime ,
status tinyint(4),
parcel_status tinyint(4),
has_invoice tinyint(1),
num_invoice varchar(20),
exchange_rate_id int(11),
cash_register_id int(11),
total_count_guipp int(11),
total_count_guipp_remaining int(11),
promo_id int(11),
amount decimal(12,2),
discount decimal(12,2),
has_insurance tinyint(1),
insurance_value decimal(12,2),
insurance_amount decimal(12,2),
extra_charges decimal(12,2),
iva decimal(12,2),
parcel_iva decimal(12,2),
total_amount decimal(12,2),
canceled_at datetime,
canceled_by int(11),
payback decimal(12,2),
schedule_route_destination_id int(11),
debt float(12,2),
invoice_id int(11),
parcels_cancel_reason_id int(11),
invoice_is_global bit(1),
cancel_code varchar(30),
in_payment tinyint(1),
expire_at datetime not null,
FOREIGN key (invoice_id) REFERENCES invoice(id),
FOREIGN key (promo_id) REFERENCES promos(id)

);

create table IF NOT EXISTS parcels_prepaid_detail(
id int(11) primary key unique not null auto_increment,
guiapp_code varchar(60) not null,
parcel_prepaid_id int(11) not null,
parcel_id int(11) ,
ticket_id int(11) ,
branchoffice_id_exchange int(11),
customer_id_exchange int (11),
price_km double not null,
price_km_id int(11) not null,
price   double not null,
price_id int(11) not null,
amount decimal(12,2),
discount decimal(12,2),
total_amount decimal(12,2),
crated_by int(11),
created_at datetime default current_timestamp,
updated_by int(11),
updated_at datetime ,
status tinyint(4),
schedule_route_destination_id int(11),
parcel_status tinyint(4),
canceled_at datetime,
canceled_by int(11),
package_type_id int(11),
expire_at datetime not null,
FOREIGN key (parcel_prepaid_id) REFERENCES parcels_prepaid(id),
FOREIGN key (branchoffice_id_exchange) REFERENCES branchoffice(id),
FOREIGN key (ticket_id) REFERENCES tickets(id),
FOREIGN key (customer_id_exchange) REFERENCES customer(id),
FOREIGN key (price_km_id) REFERENCES pp_price_km(id),
FOREIGN key (price_id) REFERENCES pp_price(id),
FOREIGN key (package_type_id) REFERENCES package_types(id)
);

ALTER TABLE `payment`
ADD COLUMN `parcels_prepaid_id` INT(11) NULL AFTER `is_extra_charge`,
ADD INDEX `fk_parcels_prepaid_id_idx` (`parcels_prepaid_id` ASC);

ALTER TABLE `payment`
ADD CONSTRAINT `fk_parcels_prepaid_id`
  FOREIGN KEY (`parcels_prepaid_id`)
  REFERENCES `parcels_prepaid` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;


ALTER TABLE `tickets`
ADD COLUMN `parcel_prepaid_id` INT(11) NULL AFTER `is_global_invoice`,
ADD INDEX `fk_parcel_prepaid_id_idx` (`parcel_prepaid_id` ASC);
;
ALTER TABLE `tickets`
ADD CONSTRAINT `fk_parcel_prepaid_id`
  FOREIGN KEY (`parcel_prepaid_id`)
  REFERENCES `parcels_prepaid` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;
