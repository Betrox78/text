-- 1675899164 UP create table users promos
CREATE TABLE IF NOT EXISTS users_promos(
	id int(11) AUTO_INCREMENT PRIMARY KEY,
	promo_id int(11) NOT NULL,
	user_id int(11) NOT NULL,
	usage_limit int(11) NOT NULL DEFAULT 0,
	used int(11) NOT NULL DEFAULT 0,
	status int(11) NOT NULL DEFAULT 1,
	created_at datetime NULL DEFAULT CURRENT_TIMESTAMP,
	created_by int(11) NOT NULL,
	updated_at datetime DEFAULT NULL,
	updated_by int(11) DEFAULT NULL,
	CONSTRAINT fk_users_promos_promo_id FOREIGN KEY (promo_id) REFERENCES promos(id) ON DELETE CASCADE,
	CONSTRAINT fk_users_promos_customer_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
	UNIQUE compuest_promo_id_user_id_idx(promo_id, user_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8;