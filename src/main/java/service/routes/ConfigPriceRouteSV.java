/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.routes;

import database.routes.ConfigPriceRouteDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class ConfigPriceRouteSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return ConfigPriceRouteDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/configPricesRoutes";
    }

}
