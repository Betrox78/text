/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.geo;

import database.commons.DBVerticle;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class StreetDBV extends DBVerticle {
    @Override
    public String getTableName() {
        return "street";
    }

}
