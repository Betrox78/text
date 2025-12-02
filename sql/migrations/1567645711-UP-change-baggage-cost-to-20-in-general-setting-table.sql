-- 1567645711 UP change baggage cost to 20 in general setting table
UPDATE general_setting SET value = '20', value_default = '20' WHERE FIELD = 'baggage_cost';
