-- 1670091043 UP add work type for tractors in vehicle table
ALTER TABLE vehicle
MODIFY COLUMN work_type enum(
        '0',
        '1',
        '2',
        '3',
        '4'
    )
NOT NULL;