/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.money;

import database.commons.DBVerticle;

/**
 *
 * @author ricardomorales
 */
public class BillDBV extends DBVerticle {

    @Override
    public String getTableName() {
        return "bill";
    }

}
