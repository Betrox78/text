package service.integrationpartners;

import service.commons.ServiceVerticle;

public class IntegrationPartnerSV extends ServiceVerticle {
    @Override
    protected String getDBAddress() {
        return "integration_partner";
    }

    @Override
    protected String getEndpointAddress() {
        return "/integrationPartners";
    }
}
