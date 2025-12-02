package database.promos.handlers.PromosDBV.models;

public class TypeServiceCost {
    boolean includesRad;
    boolean includesEad;
    Double RADOCUCost;
    Double EADCost;

    public TypeServiceCost(Double RADOCUCost, Double EADCost) {
        this.includesRad = false;
        this.includesEad = false;
        this.RADOCUCost = RADOCUCost;
        this.EADCost = EADCost;
    }

    public TypeServiceCost(Double RADOCUCost, Double EADCost, boolean includesRad, boolean includesEad) {
        this.includesRad = includesRad;
        this.includesEad = includesEad;
        this.RADOCUCost = RADOCUCost;
        this.EADCost = EADCost;
    }

    public Double getRADOCUCost() {
        return RADOCUCost;
    }

    public void setRADOCUCost(Double RADOCUCost) {
        this.RADOCUCost = RADOCUCost;
    }

    public Double getEADCost() {
        return EADCost;
    }

    public void setEADCost(Double EADCost) {
        this.EADCost = EADCost;
    }

    public boolean isIncludesRad() {
        return includesRad;
    }

    public void setIncludesRad(boolean includesRad) {
        this.includesRad = includesRad;
    }

    public boolean isIncludesEad() {
        return includesEad;
    }

    public void setIncludesEad(boolean includesEad) {
        this.includesEad = includesEad;
    }
}
