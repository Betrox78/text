/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.authsubservices;

import database.authsubservices.AuthSubServicesDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class AuthSubServicesSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return AuthSubServicesDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/authSubServices";
    }

}
