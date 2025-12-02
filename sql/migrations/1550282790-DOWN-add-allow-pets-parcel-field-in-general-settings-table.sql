-- 1550282790 DOWN add-allow-pets-parcel-field-in-general-settings-table
DELETE FROM general_setting WHERE FIELD = 'allow_pets_parcel' AND group_type = 'pets';