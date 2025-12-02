-- 1670091043 DOWN add work type for tractors in vehicle table
ALTER TABLE vehicle
MODIFY COLUMN work_type enum(
        '0',
        '1',
        '2'
    )
NOT NULL;