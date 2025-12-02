/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.jobs;

import database.jobs.CaseFileDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class CaseFileSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return CaseFileDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/casefiles";
    }

}
