-- 1587222932 UP add-new-rules-in-promo
ALTER TABLE promos
MODIFY rule ENUM('boardingpass_abierto', 'boardingpass_redondo', 'boardingpass_abierto_sencillo', 'boardingpass_sencillo'),
ADD apply_return TinyInt(1) DEFAULT 0;

update promos set apply_return = true;
