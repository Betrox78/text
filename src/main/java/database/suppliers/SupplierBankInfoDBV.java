/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.suppliers;

import database.commons.DBVerticle;

/**
 *
 * @author ulises
 */
public class SupplierBankInfoDBV extends DBVerticle {

    @Override
    public String getTableName() {
        return "supplier_bank_info";
    }

}
