/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.configs;

import database.configs.GeneralConfigDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author ulises
 */
public class GeneralConfigSV extends ServiceVerticle{

    @Override
    protected String getDBAddress() {
        return GeneralConfigDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/generalsConfigs";
    }
    
}
