-- 1687407534 UP schedule-seat-lock-created-at-index

CREATE INDEX idx_schedule_route_destination_seat_lock_status_created_at
ON schedule_route_destination_seat_lock (status, created_at);