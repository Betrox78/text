-- 1701493104 UP shipments-parcel-package-tracking-fk-trailer
ALTER TABLE shipments_parcel_package_tracking
ADD CONSTRAINT fk_trailer_id_idx FOREIGN KEY (trailer_id) REFERENCES trailers(id) ON DELETE NO ACTION ON UPDATE NO ACTION;