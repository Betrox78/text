-- 1725140792 UP add parcels_confirm_dialogs_init_config table
ALTER TABLE parcels_init_config
DROP COLUMN enable_confirm_dialog_notes;

CREATE TABLE parcels_confirm_dialogs_init_config(
	id INTEGER PRIMARY KEY,
    employee_id INT NOT NULL,
    enable_confirm_dialog_notes BOOLEAN NOT NULL DEFAULT FALSE,
    status INTEGER DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
	created_by INTEGER DEFAULT NULL,
	updated_at DATETIME DEFAULT NULL,
	updated_by INTEGER DEFAULT NULL,
    UNIQUE unique_parcels_confirm_dialogs_init_config_employee_id_idx(employee_id),
	CONSTRAINT parcels_confirm_dialogs_init_config_employee_id_fk FOREIGN KEY (employee_id) REFERENCES employee(id) ON UPDATE NO ACTION ON DELETE NO ACTION
);