-- 1744255901 UP job-indexes
CREATE INDEX idx_job_status ON job(status);
CREATE INDEX idx_job_created_at ON job(created_at);
CREATE INDEX idx_job_updated_at ON job(updated_at);