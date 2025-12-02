-- 1732401256 UP general setting external document profile id
INSERT INTO `general_setting`
(`FIELD`, `value`, `type_field`, `description`, `value_default`, `label_text`, `explanation_text`, `group_type`, `status`, `created_at`, `created_by`)
VALUES
('external_document_profile_id', '13', 'select', 'Perfil de documentador externo', '13', 'Perfil de documentador externo', 'profiles?query=*,status=1', 'parcel', 1, NOW(), 1);