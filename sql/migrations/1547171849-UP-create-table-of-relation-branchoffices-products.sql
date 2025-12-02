-- 1547171849 UP create-table-of-relation-branchoffices-products
CREATE TABLE branchoffices_products (
  product_id int(11) NOT NULL,
  branchoffice_id int(11) NOT NULL,
  CONSTRAINT fk_product_branchoffice_id FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT fk_branchoffice_product_id FOREIGN KEY (branchoffice_id) REFERENCES branchoffice(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;