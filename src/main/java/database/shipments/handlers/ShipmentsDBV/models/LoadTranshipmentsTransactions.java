package database.shipments.handlers.ShipmentsDBV.models;

import database.commons.GenericQuery;

import java.util.ArrayList;
import java.util.List;

public class LoadTranshipmentsTransactions {
    private List<GenericQuery> creates;
    private List<GenericQuery> updates;

    public LoadTranshipmentsTransactions() {
        creates = new ArrayList<>();
        updates = new ArrayList<>();
    }

    public List<GenericQuery> getCreates() {
        return creates;
    }

    public void setCreates(List<GenericQuery> creates) {
        this.creates = creates;
    }

    public void addCreate(GenericQuery create) {
        this.creates.add(create);
    }

    public List<GenericQuery> getUpdates() {
        return updates;
    }

    public void setUpdates(List<GenericQuery> updates) {
        this.updates = updates;
    }

    public void addUpdate(GenericQuery update) {
        this.updates.add(update);
    }
}
