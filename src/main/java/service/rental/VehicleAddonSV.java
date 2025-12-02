/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.rental;

import database.rental.VehiclesAddonDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author ulises
 */
public class VehicleAddonSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return VehiclesAddonDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/vehiclesAddons";
    }

}
