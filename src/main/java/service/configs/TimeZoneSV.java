/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.configs;

import database.configs.TimeZoneDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author ulises
 */
public class TimeZoneSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return TimeZoneDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/timezones";
    }

}
