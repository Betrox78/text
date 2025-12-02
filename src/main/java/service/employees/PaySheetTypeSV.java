/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.employees;

import database.employees.PaySheetTypeDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class PaySheetTypeSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return PaySheetTypeDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/paySheetType";
    }

}
