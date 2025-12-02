-- 1565909940 DOWN add driver free complements limit in general settings
DELETE FROM general_setting WHERE FIELD = 'driver_free_complements_limit' AND group_type = 'travel';