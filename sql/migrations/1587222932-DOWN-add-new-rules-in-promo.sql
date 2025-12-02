-- 1587222932 DOWN add-new-rules-in-promo
ALTER TABLE promos
MODIFY rule ENUM('boardingpass_abierto', 'boardingpass_redondo'),
DROP apply_return;
