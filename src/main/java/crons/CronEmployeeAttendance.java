package crons;

import database.branchoffices.BranchofficeDBV;
import database.commons.Action;
import database.employees.EmployeeDBV;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static service.commons.Constants.ACTION;

public class CronEmployeeAttendance extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        super.start();
        startCron();
    }

    private void startCron() {
        this.vertx.setPeriodic(1000 * 60 * 60 * 24, delay -> {
            System.out.println("Running employee aggregate attendance cron :".concat(new Date().toString()));

            getEmployees().setHandler(reply -> {
               try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    JsonArray employees = reply.result();
                    System.out.println("Total employees: ".concat(String.valueOf(employees.size())));
                    nextAggregate(employees, 0);

               } catch (Exception ex) {
                    ex.printStackTrace();
               }
            });
        });
    }

    private Future<JsonArray> getEmployees() {
        Future<JsonArray> future = Future.future();
        try {
            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, EmployeeDBV.ACTION_GET_EMPLOYEES);

            this.vertx.eventBus().send(EmployeeDBV.class.getSimpleName(), new JsonObject(), options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    future.complete((JsonArray) reply.result().body());

                } catch (Exception ex) {
                    ex.printStackTrace();
                    future.fail(ex);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            future.fail(ex);
        }

        return future;
    }


    private Future<JsonObject> nextAggregate(JsonArray list, int index) {
        Future<JsonObject> future = Future.future();
        int next = index + 1;

        try {
            if (list.size() == index) {
                System.out.println("All employees processed");
                future.complete();
                return future;
            }

            JsonObject employee = list.getJsonObject(index);
            aggregateEmployee(employee).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    nextAggregate(list, next);
                    future.complete();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    nextAggregate(list, next);
                    future.complete();
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            future.complete();
            if (list.size() == index) {
                System.out.println("All employees processed");
                return future;
            }
            nextAggregate(list, next);
        }

        return future;
    }

    private Future<JsonObject> aggregateEmployee(JsonObject employee) {
        Future<JsonObject> future = Future.future();
        try {

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, EmployeeDBV.ACTION_AGGREGATE_ATTENDANCE_DAY);

            this.vertx.eventBus().send(EmployeeDBV.class.getSimpleName(), employee, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    future.complete((JsonObject) reply.result().body());

                } catch (Exception ex) {
                    ex.printStackTrace();
                    future.fail(ex);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            future.fail(ex);
        }

        return future;
    }

}
