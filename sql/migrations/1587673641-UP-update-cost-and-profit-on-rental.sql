-- 1587673641 UP update-cost-and-profit-on-rental
UPDATE rental AS r
	LEFT JOIN general_setting AS gsi ON gsi.FIELD = "iva"
    LEFT JOIN general_setting AS gsp ON gsp.FIELD = "extra_earning_percent"
	SET r.profit = (r.amount/(1 + (CONVERT( gsi.value, decimal(12,2)) / 100))) + ((r.driver_cost + r.extra_charges + r.checklist_charges)/(1 + (CONVERT( gsi.value, decimal(12,2)) / 100))) * (CONVERT( gsp.value, decimal(12,2)) / 100),
    r.cost = ((r.driver_cost + r.extra_charges + r.checklist_charges)/(1 + (CONVERT( gsi.value, decimal(12,2)) / 100))) /(1 + (CONVERT( gsp.value, decimal(12,2)) / 100))
    where (r.profit = 0 AND r.cost = 0);