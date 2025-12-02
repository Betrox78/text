/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.money;

import database.money.CashOutMoveDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author ulises
 */
public class CashOutMoveSV extends ServiceVerticle{

    @Override
    protected String getDBAddress() {
        return CashOutMoveDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/cashOutsMoves";
    }
    
}
