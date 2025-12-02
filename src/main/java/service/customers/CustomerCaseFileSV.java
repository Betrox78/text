/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.customers;

import database.customers.CustomerCaseFileDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author ulises
 */
public class CustomerCaseFileSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return CustomerCaseFileDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/customersCasefiles";
    }

}
