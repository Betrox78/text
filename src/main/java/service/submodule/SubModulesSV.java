/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.submodule;

import database.submodule.SubModuleDBV;
import service.commons.ServiceVerticle;

/**
 *
 * Kriblet
 */
public class SubModulesSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return SubModuleDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/subModules";
    }

}
