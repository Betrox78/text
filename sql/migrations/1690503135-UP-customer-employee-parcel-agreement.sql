-- 1690503135 UP customer-employee-parcel-agreement
ALTER TABLE customer
ADD COLUMN parcels_seller_employee_id INT;

ALTER TABLE customer
ADD CONSTRAINT fk_customer_parcels_seller_employee_id
FOREIGN KEY (parcels_seller_employee_id) REFERENCES employee(id);

-- Hazael
UPDATE customer 
SET parcels_seller_employee_id = 
    (SELECT id FROM employee WHERE user_id = 10914)
WHERE id IN (
    35494, 106459, 35503, 
    60766, 61109, 65433, 61234,
    88571,
    51594, 88306,
    56591, 110956, 110959, 110957, 110962, 110960, 110964,
    98282, 98283, 109280
);

-- Lucia
UPDATE customer 
SET parcels_seller_employee_id = 
    (SELECT id FROM employee WHERE user_id = 13750)
WHERE id IN (
    114311
);

-- Luis
UPDATE customer 
SET parcels_seller_employee_id = 
    (SELECT id FROM employee WHERE user_id = 13741)
WHERE id IN (
    3421, 15659, 44278, 92608, 111594
);

-- Juan Carlos
UPDATE customer 
SET parcels_seller_employee_id = 
    (SELECT id FROM employee WHERE user_id = 11072)
WHERE id IN (
    65436,
    28356, 23248, 110970,
    115928, 128391, 130054, 115852,
    59540
);

-- Kareli
UPDATE customer 
SET parcels_seller_employee_id = 
    (SELECT id FROM employee WHERE user_id = 10937)
WHERE id IN (
    117609,
    121649,
    66016, 66017
);

-- Daniel
UPDATE customer 
SET parcels_seller_employee_id = 
    (SELECT id FROM employee WHERE user_id = 112)
WHERE id IN (
    76540,
    54764,
    109284,
    84734, 84735,
    17171, 20999,
    25848,
    125278, 128216,
    43640
);

-- CORP
-- UPDATE customer 
-- SET parcels_seller_employee_id = 
--     (SELECT id FROM employee WHERE user_id = ?)
-- WHERE id IN (
--     ?,
--     ?,
-- );

-- Jose
UPDATE customer 
SET parcels_seller_employee_id = 
    (SELECT id FROM employee WHERE user_id = 13347)
WHERE id IN (
    109862,
    110855,
    110783,
    110631,
    112318,
    112319,
    116290, 116698,
    107808,
    118175,
    107819,
    119184,
    121092,
    120155,
    115200, 115310,
    113381, 113440,
    116225, 113362,
    111653,
    117386,
    112225,
    120174, 125281,
    73791,
    128199,
    121226
);

-- Jesus
UPDATE customer 
SET parcels_seller_employee_id = 
    (SELECT id FROM employee WHERE user_id = 14233)
WHERE id IN (
    118374,
    117164,
    119036, 117560,
    116417, 11519, 120904,
    118993,
    116589,
    113483, 113524,
    115402,
    117375,
    118364, 117613,
    118962,
    123748,
    124088,
    119135,
    125569, 126775,
    129082,
    111300
);

-- Claudia
UPDATE customer 
SET parcels_seller_employee_id = 
    (SELECT id FROM employee WHERE user_id = 14676)
WHERE id IN (
    111295,
    115650,
    123477
);