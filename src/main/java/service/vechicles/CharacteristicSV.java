/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.vechicles;

import database.vechicle.CharacteristicDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author ulises
 */
public class CharacteristicSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return CharacteristicDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/characteristics";
    }

}
