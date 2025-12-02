/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.employees;

import database.employees.EmployeeCaseFileDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class EmployeeCasefileSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return EmployeeCaseFileDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/employeeCasefiles";
    }

}
