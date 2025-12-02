-- 1645551104 DOWN add-new-values-to-enum
ALTER TABLE parcel_invoice_complement MODIFY COLUMN tipo_cfdi
enum('traslado sin complemento carta porte','traslado con complemento carta porte','ingreso','ingreso con complemento carta porte');

ALTER TABLE parcel_invoice_complement MODIFY COLUMN system_origin
enum('bitacora','app','site');

ALTER TABLE boardingpass_invoice_complement MODIFY COLUMN tipo_cfdi
enum('traslado sin complemento carta porte','traslado con complemento carta porte','ingreso','ingreso con complemento carta porte');


ALTER TABLE boardingpass_invoice_complement MODIFY COLUMN system_origin
enum('bitacora','app','site');