-- 1691219153 DOWN e-wallet-generalsettings
DELETE from general_setting gs where gs.FIELD = 'parcel_e_wallet_percent' AND gs.id > 0;
DELETE from general_setting gs where gs.FIELD = 'bonus_reference_e_wallet_percent' AND gs.id > 0;
