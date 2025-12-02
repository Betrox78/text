-- 1555461157 UP add extra earning percent in general setting table
INSERT INTO `general_setting` (`FIELD`, `value`, `type_field`, `required_field`, `description`, `value_default`, `label_text`, `order_field`, `group_type`, `type_setting`)
VALUES ('extra_earning_percent', '10', 'percent', '0', 'Ganancia sobre cargos extras', '0', 'Ganancia sobre cargos extras', 0,'rental', '0');