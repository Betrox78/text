/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.jobs;

import database.commons.DBVerticle;
import database.commons.ErrorCodes;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import service.commons.Constants;

import static service.commons.Constants.INVALID_DATA;
import static service.commons.Constants.INVALID_DATA_MESSAGE;
import static utils.UtilsResponse.*;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class JobDBV extends DBVerticle {

    public static final String JOB_WITH_REQ = "JobDBV.jobWithRequirements";

    @Override
    public String getTableName() {
        return "job";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(Constants.ACTION);
        switch (action) {
            case JOB_WITH_REQ:
                this.reportWithRequirements(message);
                break;
        }
    }

    private void reportWithRequirements(Message<JsonObject> message) {
        int id = message.body().getInteger("id");
        String query = "select * from " + this.getTableName() + " where id = ?";
        JsonArray params = new JsonArray().add(id);
        dbClient.queryWithParams(query, params, reply -> {
            try{
                if(reply.failed()) {
                    throw new Exception(reply.cause());
                }
                if (reply.result().getNumRows() > 0) {
                    JsonObject job = reply.result().getRows().get(0);
                    this.dbClient.queryWithParams(QUERY_JOBS_WITH_REQ, new JsonArray().add(id), replyRep -> {
                        try{
                            if(reply.failed()){
                                throw  new Exception(reply.cause());
                            }
                            job.put("requirements", replyRep.result().getRows());
                            message.reply(job);

                        }catch(Exception e){
                            reportQueryError(message, replyRep.cause());

                        }

                    });
                } else {
                    message.reply(null);
                }
            }catch (Exception ex) {
                ex.printStackTrace();
                reportQueryError(message, reply.cause());
            }
        });
    }

    /**
     * Query to generate the detail of the job with all of its requirements
     */
    private static final String QUERY_JOBS_WITH_REQ
            = "select id,\n"
            + "       is_group,\n"
            + "       is_recurrent,\n"
            + "       parent_id,\n"
            + "       name,\n"
            + "       description,\n"
            + "       is_required,\n"
            + "       type_values,\n"
            + "       order_req,\n"
            + "       recurrent_type\n"
            + "from job_requirement \n"
            + "where job_id = ? and status = 1";

}
