-- 1567645711 DOWN change baggage cost to 20 in general setting table
UPDATE general_setting SET value = '50', value_default = '50' WHERE FIELD = 'baggage_cost';
