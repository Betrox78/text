-- 1691195555 DOWN e-wallet-bp-generalsetting
DELETE from general_setting gs where gs.FIELD = 'boarding_pass_e_wallet_percent' AND gs.id > 0;
