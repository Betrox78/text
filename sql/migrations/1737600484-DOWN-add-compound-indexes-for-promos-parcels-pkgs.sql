-- 1737600484 DOWN add-compound-indexes-for-promos-parcels-pkgs
DROP INDEX idx_parcels_packages_promo_excess ON parcels_packages;
DROP INDEX idx_promos_id_apply ON promos;