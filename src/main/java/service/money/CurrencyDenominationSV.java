/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.money;

import database.money.CurrencyDenominationDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author ulises
 */
public class CurrencyDenominationSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return CurrencyDenominationDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/currenciesDenominations";
    }
    
}
