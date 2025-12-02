/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.vechicles;

import database.vechicle.TypeVehicleDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class TypeVehicleSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return TypeVehicleDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/typeVehicles";
    }

}
