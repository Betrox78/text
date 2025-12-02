/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.boardingpass;

import database.boardingpass.BoardingPassComplementDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author ulises
 */
public class BoardingPassComplementSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return BoardingPassComplementDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/boardingPassesComplements";
    }

}
