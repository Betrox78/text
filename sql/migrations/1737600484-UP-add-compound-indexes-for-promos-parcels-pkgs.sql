-- 1737600484 UP add-compound-indexes-for-promos-parcels-pkgs
CREATE INDEX idx_parcels_packages_promo_excess ON parcels_packages(promo_id, excess_promo_id);
CREATE INDEX idx_promos_id_apply ON promos(id, apply_rad, apply_ead);