package database.promos.enums;

public enum SERVICES {
    boardingpass("boarding_pass", "boarding_pass_ticket", 2),
    parcel("parcels", "parcels_packages", 2),
    parcel_inhouse("parcels", "parcels_packages", 2),
    rental("rental", null, 2),
    guiapp("guiapp", "parcels_prepaid_detail", 2);

    private final String table;
    private final String productTable;
    private final int moneyRoundPlaces;

    SERVICES(String table, String productTable, int moneyRoundPlaces) {
        this.table = table;
        this.productTable = productTable;
        this.moneyRoundPlaces = moneyRoundPlaces;
    }

    public String getTable() {
        return table;
    }

    public String getProductTable() {
        return productTable;
    }

    public int getMoneyRoundPlaces() { return moneyRoundPlaces; }

    public boolean isPackageService(){
        return this.equals(parcel) || this.equals(guiapp) || this.equals(parcel_inhouse);
    }
}