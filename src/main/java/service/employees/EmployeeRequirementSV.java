/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.employees;

import database.employees.EmployeeRequirementDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author ulises
 */
public class EmployeeRequirementSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return EmployeeRequirementDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/employeesRquirements";
    }

}
