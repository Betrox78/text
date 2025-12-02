-- 1588707061 UP create-table-delivery-attempt
CREATE TABLE IF NOT EXISTS delivery_attempt_reason(
  id int(11) NOT NULL AUTO_INCREMENT,
  name varchar(50) NOT NULL,
  status tinyint(4) NOT NULL DEFAULT 1,
  created_at datetime NULL DEFAULT CURRENT_TIMESTAMP,
  created_by int(11) NOT NULL,
  updated_at datetime ,
  updated_by int(11) ,
  PRIMARY KEY (id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;