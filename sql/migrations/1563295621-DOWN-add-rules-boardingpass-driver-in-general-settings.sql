-- 1563295621 DOWN add rules boardingpass driver in general settings
DELETE FROM general_setting WHERE FIELD IN ('driver_max_complements', 'driver_extra_cost_complement') AND group_type = 'travel';