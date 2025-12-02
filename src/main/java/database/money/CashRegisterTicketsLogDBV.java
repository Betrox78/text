package database.money;

import database.commons.DBVerticle;

public class CashRegisterTicketsLogDBV extends DBVerticle {
    @Override
    public String getTableName() {
        return "cash_register_tickets_log";
    }
}
