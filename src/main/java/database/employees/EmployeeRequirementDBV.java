/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.employees;

import database.commons.DBVerticle;

/**
 *
 * @author ulises
 */
public class EmployeeRequirementDBV extends DBVerticle {

    @Override
    public String getTableName() {
        return "employee_requirement";
    }

}
