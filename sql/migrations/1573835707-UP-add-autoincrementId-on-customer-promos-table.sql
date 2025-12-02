-- 1573835707 UP add-autoincrementId-on-customer-promos-table
ALTER TABLE customers_promos
    ADD COLUMN id INT(11) AUTO_INCREMENT PRIMARY KEY FIRST;