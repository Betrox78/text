package database.promos.handlers.PromosDBV.models;

public class SubstractServices {

    double total;
    double radAmount;
    double eadAmount;

    public SubstractServices(double total, double radAmount, double eadAmount) {
        this.total = total;
        this.radAmount = radAmount;
        this.eadAmount = eadAmount;
    }

    public double getTotal() {
        return total;
    }

    public double getRadAmount() {
        return radAmount;
    }

    public void setRadAmount(double radAmount) {
        this.radAmount = radAmount;
    }

    public double getEadAmount() {
        return eadAmount;
    }

    public void setEadAmount(double eadAmount) {
        this.eadAmount = eadAmount;
    }

    public void checkBreakdown(double servicesAmountBeforeIva, double ivaPercent, boolean flagPromoApplyRad, boolean flagPromoApplyEad) {
        if (servicesAmountBeforeIva > 0 && (flagPromoApplyRad || flagPromoApplyEad)) {
            if (this.getRadAmount() > 0 && this.getEadAmount() > 0) {
                double servicesAmountSplited = servicesAmountBeforeIva / 2;
                this.setRadAmount(servicesAmountSplited);
                this.setEadAmount(servicesAmountSplited);
            } else if(this.getRadAmount() > 0) {
                this.setRadAmount(servicesAmountBeforeIva);
            } else if(this.getEadAmount() > 0) {
                this.setEadAmount(servicesAmountBeforeIva);
            }
        } else {
            this.setRadAmount(this.getRadAmount() / (ivaPercent + 1));
            this.setEadAmount(this.getEadAmount() / (ivaPercent + 1));
        }
    }
}
