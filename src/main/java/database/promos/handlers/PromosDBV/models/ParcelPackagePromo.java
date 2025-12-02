package database.promos.handlers.PromosDBV.models;

public class ParcelPackagePromo {
    Promo promo;
    ParcelPackage parcelPackage;

    public ParcelPackagePromo(ParcelPackage parcelPackage) {
        this.parcelPackage = parcelPackage;
        this.promo = null;
    }

    public Promo getPromo() {
        return promo;
    }

    public void setPromo(Promo promo) {
        this.promo = promo;
    }

    public ParcelPackage getParcelPackage() {
        return parcelPackage;
    }
}
