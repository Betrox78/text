/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.money;

import database.money.BillDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author ricardomorales
 */
public class BilSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return BillDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/bills";
    }

}
