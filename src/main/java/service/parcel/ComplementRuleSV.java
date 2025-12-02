package service.parcel;

import database.parcel.ComplementRuleDBV;
import service.commons.ServiceVerticle;

public class ComplementRuleSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ComplementRuleDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/complementRules";
    }

}
