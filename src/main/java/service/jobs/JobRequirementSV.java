/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.jobs;

import database.jobs.JobRequirementDBV;
import service.commons.ServiceVerticle;

/**
 *
 * @author ulises
 */
public class JobRequirementSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return JobRequirementDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/jobsRequirements";
    }

}
