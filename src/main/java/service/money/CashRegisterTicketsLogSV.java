package service.money;

import database.money.CashRegisterTicketsLogDBV;
import service.commons.ServiceVerticle;

public class CashRegisterTicketsLogSV extends ServiceVerticle {
    @Override
    protected String getDBAddress() {
        return CashRegisterTicketsLogDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/cashRegisterTicketsLog";
    }
}
