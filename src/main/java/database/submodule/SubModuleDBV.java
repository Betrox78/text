/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.submodule;

import database.commons.DBVerticle;

/**
 *
 * Kriblet
 */
public class SubModuleDBV extends DBVerticle {

    @Override
    public String getTableName() {
        return "sub_module";
    }

}
