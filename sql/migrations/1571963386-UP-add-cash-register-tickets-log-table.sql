-- 1571963386 UP add-cash-register-tickets-log-table
CREATE TABLE IF NOT EXISTS cash_register_tickets_log
(
    id               int(11)    NOT NULL AUTO_INCREMENT,
    cash_register_id int(11)    NOT NULL,
    original_ticket  int(11)    NOT NULL,
    new_ticket       int(11)    NOT NULL,
    notes            text       NULL,
    status           tinyint(4) NOT NULL default 1,
    created_at       datetime   NULL     DEFAULT CURRENT_TIMESTAMP,
    created_by       int(11)    NOT NULL,
    updated_at       datetime            DEFAULT NULL,
    updated_by       int(11)             DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;