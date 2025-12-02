package database.parcel.handlers.ParcelsManifestDBV.Exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ParcelManifestException extends Exception {
    String message;

    public ParcelManifestException(String message) {
        super();
        this.message = message;
    }
}
