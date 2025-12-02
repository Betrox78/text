/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.parcel;

import database.parcel.PetsSizesDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author Saul
 */
public class PetsSizesSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return PetsSizesDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/petsSizes";
    }
}
