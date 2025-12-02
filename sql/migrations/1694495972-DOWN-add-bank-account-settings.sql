-- 1694495972 DOWN add-bank-account-settings
DELETE from general_setting gs where gs.FIELD = 'boarding_pass_bank_acc' AND gs.id > 0;
DELETE from general_setting gs where gs.FIELD = 'boarding_pass_bank' AND gs.id > 0;
DELETE from general_setting gs where gs.FIELD = 'parcel_bank_acc' AND gs.id > 0;
DELETE from general_setting gs where gs.FIELD = 'parcel_bank' AND gs.id > 0;
