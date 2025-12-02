package service.money;

import database.money.ExpenseDBV;
import service.commons.ServiceVerticle;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class ExpenseSV extends ServiceVerticle{

    @Override
    protected String getDBAddress() {
        return ExpenseDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/expenses";
    }
    
}
