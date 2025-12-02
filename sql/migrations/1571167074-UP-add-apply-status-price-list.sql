-- 1571167074 UP add-apply-status-price-list
ALTER TABLE prices_lists
ADD COLUMN apply_status enum('pending', 'ok', 'error') DEFAULT 'ok'