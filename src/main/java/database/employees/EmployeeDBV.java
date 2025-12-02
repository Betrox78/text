/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.employees;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import database.commons.Status;
import database.employees.handlers.EmployeeDBV.*;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.joda.time.Period;
import utils.UtilsDate;

import java.util.*;

import static service.commons.Constants.ACTION;
import static service.commons.Constants._BRANCHOFFICE_ID;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class EmployeeDBV extends DBVerticle {

    public static final String EMPLOYEES_BY_JOB = "EmployeeDBV.employeesByJob";
    public static final String REGISTER = "EmployeeDBV.register";
    public static final String ACTION_EMPLOYEE_BY_USERE_ID = "EmployeeDBV.employeeByUserId";
    public static final String ACTION_AVALAIBLE_EMPLOYEES = "EmployeeDBV.avalaibleEmployees";
    public static final String ACTION_AVAILABLE_VAN_DRIVER = "EmployeeDBV.availableVanDriver";
    public static final String FINGER_PRINT_REGISTER = "EmployeeDBV.fingerPrintRegister";
    public static final String ACTION_REGISTER_ATTENDANCE = "EmployeeDBV.registerAttendance";
    public static final String ACTION_GET_EMPLOYEES = "EmployeeDBV.getEmployees";
    public static final String ACTION_AGGREGATE_ATTENDANCE_DAY = "EmployeeDBV.aggregateAttendanceDay";
    public static final String ACTION_GET_ATTENDANCE_REPORT = "EmployeeDBV.getAttendanceReport";
    public static final String ACTION_ATTENDANCE_DRIVER_REPORT = "EmployeeDBV.attendanceDriverReport";
    public static final String ACTION_SEARCH_BY_NAME_AND_LASTNAME = "EmployeeDBV.searchEmployeeByName";
    public static final String ACTION_GET_EMPLOYEES_OPERATOR = "EmployeeDBV.getEmployeesOperator";
    public static final String ACTION_AVALAIBLE_EMPLOYEES_BY_JOB_ID = "EmployeeDBV.avalaibleEmployeesByJobId";
    public static final String ACTION_REGISTER_PARCEL_INIT_CONFIG = "EmployeeDBV.registerParcelInitConfig";
    public static final String ACTION_UPDATE_PARCEL_INIT_CONFIG = "EmployeeDBV.updateParcelInitConfig";
    public static final String ACTION_GET_PARCEL_INIT_CONFIG = "EmployeeDBV.getParcelInitConfig";
    public static final String ACTION_DELETE_PARCEL_INIT_CONFIG = "EmployeeDBV.deleteParcelInitConfig";
    public static final String ACTION_REGISTER_PARCEL_BRANCHOFFICE_INIT_CONFIG = "EmployeeDBV.registerParcelBranchofficeInitConfig";
    public static final String ACTION_UPDATE_PARCEL_BRANCHOFFICE_INIT_CONFIG = "EmployeeDBV.updateParcelBranchofficeInitConfig";
    public static final String ACTION_GET_PARCEL_BRANCHOFFICE_INIT_CONFIG = "EmployeeDBV.getParcelBranchofficeInitConfig";
    public static final String ACTION_REGISTER_PARCEL_CONFIRM_DIALOGS_INIT_CONFIG = "EmployeeDBV.registerParcelConfirmDialogsInitConfig";
    public static final String ACTION_UPDATE_PARCEL_CONFIRM_DIALOGS_INIT_CONFIG = "EmployeeDBV.updateParcelConfirmDialogsInitConfig";
    public static final String ACTION_GET_PARCEL_CONFIRM_DIALOGS_INIT_CONFIG = "EmployeeDBV.getParcelConfirmDialogsInitConfig";
    public static final String ACTION_DELETE_PARCEL_CONFIRM_DIALOGS_INIT_CONFIG = "EmployeeDBV.deleteParcelConfirmDialogsInitConfig";
    public static final String ACTION_REGISTER_EXCHANGE_INIT_CONFIG = "EmployeeDBV.registerExchangeInitConfig";
    public static final String ACTION_UPDATE_EXCHANGE_INIT_CONFIG = "EmployeeDBV.updateExchangeInitConfig";
    public static final String ACTION_GET_EXCHANGE_INIT_CONFIG = "EmployeeDBV.getExchangeInitConfig";
    public static final String ACTION_DELETE_EXCHANGE_INIT_CONFIG = "EmployeeDBV.deleteExchangeInitConfig";

    @Override
    public String getTableName() {
        return "employee";
    }

    ParcelConfigRegister parcelConfigRegister;
    ParcelConfigUpdate parcelConfigUpdate;
    ParcelConfigFind parcelConfigFind;
    ParcelConfigDelete parcelConfigDelete;

    ParcelBranchofficeConfigRegister parcelBranchofficeConfigRegister;
    ParcelBranchofficeConfigUpdate parcelBranchofficeConfigUpdate;
    ParcelBranchofficeConfigFind parcelBranchofficeConfigFind;

    ParcelConfirmDialogsConfigRegister parcelConfirmDialogsConfigRegister;
    ParcelConfirmDialogsConfigUpdate parcelConfirmDialogsConfigUpdate;
    ParcelConfirmDialogsConfigFind parcelConfirmDialogsConfigFind;

    ExchangeConfig exchangeConfig;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        this.parcelConfigRegister = new ParcelConfigRegister(this);
        this.parcelConfigUpdate = new ParcelConfigUpdate(this);
        this.parcelConfigFind = new ParcelConfigFind(this);
        this.parcelConfigDelete = new ParcelConfigDelete(this);
        this.parcelBranchofficeConfigRegister = new ParcelBranchofficeConfigRegister(this);
        this.parcelBranchofficeConfigUpdate = new ParcelBranchofficeConfigUpdate(this);
        this.parcelBranchofficeConfigFind = new ParcelBranchofficeConfigFind(this);
        this.parcelConfirmDialogsConfigRegister = new ParcelConfirmDialogsConfigRegister(this);
        this.parcelConfirmDialogsConfigUpdate = new ParcelConfirmDialogsConfigUpdate(this);
        this.parcelConfirmDialogsConfigFind = new ParcelConfirmDialogsConfigFind(this);
        this.exchangeConfig = new ExchangeConfig(this);
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case EMPLOYEES_BY_JOB:
                employeesByJob(message);
                break;
            case REGISTER:
                register(message);
                break;
            case ACTION_EMPLOYEE_BY_USERE_ID:
                getEmployeeByUserId(message);
                break;
            case ACTION_AVALAIBLE_EMPLOYEES:
                avalaibleEmployees(message);
                break;
            case ACTION_AVAILABLE_VAN_DRIVER:
                availableVanDriver(message);
                break;
            case FINGER_PRINT_REGISTER:
                fingerPrintRegister(message);
                break;
            case ACTION_REGISTER_ATTENDANCE:
                registerAttendance(message);
                break;
            case ACTION_GET_EMPLOYEES:
                getEmployees(message);
                break;
            case ACTION_AGGREGATE_ATTENDANCE_DAY:
                aggregateAttendanceDay(message);
                break;
            case ACTION_GET_ATTENDANCE_REPORT:
                getAttendanceReport(message);
                break;
            case ACTION_ATTENDANCE_DRIVER_REPORT:
                driverAttendanceReport(message);
                break;
            case ACTION_SEARCH_BY_NAME_AND_LASTNAME:
                searchEmployeeByName(message);
                break;
            case ACTION_GET_EMPLOYEES_OPERATOR:
                getEmployeesOperator(message);
                break;
            case ACTION_AVALAIBLE_EMPLOYEES_BY_JOB_ID:
                avalaibleEmployeesByJobId(message);
                break;
            case ACTION_REGISTER_PARCEL_INIT_CONFIG:
                this.parcelConfigRegister.handle(message);
                break;
            case ACTION_UPDATE_PARCEL_INIT_CONFIG:
                this.parcelConfigUpdate.handle(message);
                break;
            case ACTION_GET_PARCEL_INIT_CONFIG:
                this.parcelConfigFind.handle(message);
                break;
            case ACTION_DELETE_PARCEL_INIT_CONFIG:
                this.parcelConfigDelete.handle(message);
                break;
            case ACTION_REGISTER_PARCEL_BRANCHOFFICE_INIT_CONFIG:
                this.parcelBranchofficeConfigRegister.handle(message);
                break;
            case ACTION_UPDATE_PARCEL_BRANCHOFFICE_INIT_CONFIG:
                this.parcelBranchofficeConfigUpdate.handle(message);
                break;
            case ACTION_GET_PARCEL_BRANCHOFFICE_INIT_CONFIG:
                this.parcelBranchofficeConfigFind.handle(message);
                break;
            case ACTION_REGISTER_PARCEL_CONFIRM_DIALOGS_INIT_CONFIG:
                this.parcelConfirmDialogsConfigRegister.handle(message);
                break;
            case ACTION_UPDATE_PARCEL_CONFIRM_DIALOGS_INIT_CONFIG:
                this.parcelConfirmDialogsConfigUpdate.handle(message);
                break;
            case ACTION_GET_PARCEL_CONFIRM_DIALOGS_INIT_CONFIG:
                this.parcelConfirmDialogsConfigFind.handle(message);
                break;
            case ACTION_REGISTER_EXCHANGE_INIT_CONFIG:
                this.exchangeConfig.register(message);
                break;
            case ACTION_UPDATE_EXCHANGE_INIT_CONFIG:
                this.exchangeConfig.update(message);
                break;
            case ACTION_GET_EXCHANGE_INIT_CONFIG:
                this.exchangeConfig.find(message);
                break;
            case ACTION_DELETE_EXCHANGE_INIT_CONFIG:
                this.exchangeConfig.delete(message);
                break;
        }
    }

    private void getAttendanceReport(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer branchofficeID = body.getInteger("branchoffice_id");
            String startDate = body.getString("start_date");
            String endDate = body.getString("end_date");

            String query = "SELECT ead.employee_id AS id, emp.curp, emp.nss, emp.name, emp.last_name, " +
                    "COUNT(ead.employee_id) AS days, SUM(IF(ead.attendance_status = 'absence',1, 0)) AS absence, " +
                    "SUM(IF(ead.attendance_status = 'attendance',1, 0)) AS attendance, " +
                    "SUM(IF(ead.attendance_status = 'late',1, 0)) AS late " +
                    "FROM employee_attendance_day AS ead " +
                    "INNER JOIN employee AS emp ON emp.id = ead.employee_id " +
                    "WHERE emp.branchoffice_id = ? AND ead.attendance_day BETWEEN ? AND ? " +
                    "GROUP BY id;";

            JsonArray params = new JsonArray()
                    .add(branchofficeID)
                    .add(startDate)
                    .add(endDate);

            this.dbClient.queryWithParams(query, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    message.reply(new JsonArray(reply.result().getRows()));

                } catch (Exception ex) {
                    ex.printStackTrace();
                    reportQueryError(message, ex);
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private  void fingerPrintRegister(Message<JsonObject> message){
        try {
            Integer userId = message.body().getInteger("user_id");
            String fingerPrint = message.body().getString("finger_print");
            if(!fingerPrint.isEmpty()) {
                byte[] finger_print = fingerPrint.getBytes();
                JsonArray params = new JsonArray().add(finger_print).add(userId);
                this.dbClient.updateWithParams(UPDATE_EMPLOYEE_WITH_FINGER_PRINT, params, reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        Integer result = reply.result().getUpdated();
                        if (result <= 0) {
                            throw new Exception("user id not found");
                        } else {
                            message.reply(new JsonObject().put("user_id", userId));
                        }
                    } catch (Throwable t){
                        t.printStackTrace();
                        reportQueryError(message, t);
                    }
                });
            }
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void getEmployeeByUserId(Message<JsonObject> message) {
        try {
            Integer userId = message.body().getInteger("user_id");
            JsonArray params = new JsonArray().add(userId);
            this.dbClient.queryWithParams(QUERY_EMPLOYEE_BY_USER_ID, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        throw new Exception("Employee not found");
                    }
                    message.reply(result.get(0));
                } catch (Throwable t){
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void avalaibleEmployees(Message<JsonObject> message) {
        this.dbClient.query(QUER_GET_VALUE_DRIVER_BUS, repply ->{
           try{
               if(repply.failed()){
                   throw new Exception(repply.cause());
               }
               JsonObject result = repply.result().getRows().get(0);
               if(result.isEmpty()){
                   throw new Exception("General setting value for driver not found");
               }
               String idJob = result.getString("value");
               this.dbClient.queryWithParams(QUERY_AVALAIBLE_EMPLOYEES,new JsonArray().add(idJob), reply -> {
                   try {
                       if (reply.failed()){
                           throw reply.cause();
                       }
                       message.reply(new JsonArray(reply.result().getRows()));
                   } catch (Throwable t){
                       t.printStackTrace();
                       reportQueryError(message, t);
                   }
               });
           } catch (Exception ex){
               ex.printStackTrace();
               reportQueryError(message , ex);
           }
        });
    }

    private void avalaibleEmployeesByJobId(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer idJob = body.getInteger("jobId");
        this.dbClient.queryWithParams(QUERY_AVALAIBLE_EMPLOYEES,new JsonArray().add(idJob), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                message.reply(new JsonArray(reply.result().getRows()));
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }



    private void availableVanDriver(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer pickUpCityId = body.getInteger("pickup_city_id");
            Integer vanDriverId = body.getInteger("driver_van_id");

            JsonArray params = new JsonArray()
                    .add(pickUpCityId).add(vanDriverId);

            this.dbClient.queryWithParams(QUERY_AVALAIBLE_VAN_DRIVERS, params, reply -> {
                this.genericResponse(message, reply);
            });
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void employeesByJob(Message<JsonObject> message) {
        String consultaReporte = "SELECT \n" +
                "\tj.id AS job_id, \n" +
                "    j.name AS job_name, \n" +
                "    e.id, \n" +
                "    CONCAT(e.name, ' ', e.last_name) AS employee_full_name,\n" +
                "    e.email AS employee_email,\n" +
                "    e.phone AS employee_phone,\n" +
                "    e.status,\n" +
                "    b.name AS branchoffice_name,\n" +
                "    b.prefix AS branchoffice_prefix\n" +
                "FROM job j \n" +
                "INNER JOIN employee e ON e.job_id = j.id\n" +
                "INNER JOIN branchoffice b ON b.id = e.branchoffice_id\n" +
                "WHERE \n" +
                "\tj.status != 4\n" +
                "    AND e.status IN (1, 2, 3)";
        this.dbClient.query(consultaReporte, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> rows = reply.result().getRows();
                message.reply(new JsonArray(rows));
            } catch (Throwable t){
                reportQueryError(message, t);
            }
        });
    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject employee = message.body().copy();
                employee.remove("schedules");
                employee.remove("contacts");
                employee.remove("casefiles");

                this.validateField("employee", "rfc", employee.getString("rfc")).whenComplete((resultValidationRFC, errorRFC) ->{
                    try {
                        if (errorRFC != null){
                            throw errorRFC;
                        }
                        this.validateField("employee", "curp", employee.getString("curp")).whenComplete((resultValidation, errorCURP) ->{
                            try {
                                if (errorCURP != null){
                                    throw errorCURP;
                                }

                                conn.updateWithParams(QUERY_UPDATE_DELETED_EMPLOYEE, new JsonArray().add(employee.getString("email")), updateResultAsyncResult -> {
                                    try{
                                        if(updateResultAsyncResult.failed()){
                                            throw updateResultAsyncResult.cause();
                                        }

                                        GenericQuery create = this.generateGenericCreate(employee);
                                        conn.updateWithParams(create.getQuery(), create.getParams(), createHandler -> {
                                            try {
                                                if (createHandler.failed()){
                                                    throw createHandler.cause();
                                                }
                                                final int id = createHandler.result().getKeys().getInteger(0);
                                                List<String> batch = new ArrayList<>();
                                                //insert schedules
                                                JsonArray schedules = message.body().getJsonArray("schedules");
                                                if (schedules != null) {
                                                    for (int i = 0; i < schedules.size(); i++) {
                                                        JsonObject schedule = schedules.getJsonObject(i);
                                                        schedule.put("employee_id", id);
                                                        batch.add(this.generateGenericCreate("employee_schedule", schedule));
                                                    }
                                                }
                                                //insert contacts
                                                JsonArray contacts = message.body().getJsonArray("contacts");
                                                JsonObject contact;
                                                if (contacts != null) {
                                                    for (int i = 0; i < contacts.size(); i++) {
                                                        contact = contacts.getJsonObject(i);
                                                        contact.put("employee_id", id);
                                                        batch.add(this.generateGenericCreate("employee_contact", contact));
                                                    }
                                                }
                                                //insert casefiles if exists
                                                JsonArray casefiles = message.body().getJsonArray("casefiles");
                                                JsonObject casefile;
                                                if (casefiles != null) {
                                                    for (int i = 0; i < casefiles.size(); i++) {
                                                        casefile = casefiles.getJsonObject(i);
                                                        casefile.put("employee_id", id);
                                                        batch.add(this.generateGenericCreate("employee_casefile", casefile));
                                                    }
                                                }
                                                conn.batch(batch, batchReply -> {
                                                    try {
                                                        if (batchReply.failed()){
                                                            throw batchReply.cause();
                                                        }
                                                        this.commit(conn, message, new JsonObject().put("id", id));
                                                    } catch (Throwable t){
                                                        t.printStackTrace();
                                                        this.rollback(conn, t, message);
                                                    }
                                                });
                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                this.rollback(conn, t, message);
                                            }
                                        });

                                    }catch (Throwable ex){
                                        ex.printStackTrace();
                                        this.rollback(conn, ex, message);
                                    }
                                });

                            } catch (Throwable t){
                                t.printStackTrace();
                                this.rollback(conn, t, message);
                            }
                        });
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });
    }

    private void registerAttendance(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject attendance = message.body();
                Integer employeeId = attendance.getInteger("employee_id");
                conn.queryWithParams(QUERY_EMPLOYEE_BY_ID, new JsonArray().add(employeeId), replyFind -> {
                    try {
                        if (replyFind.failed()) {
                            throw new Exception(replyFind.cause());
                        }

                        List<JsonObject> employees = replyFind.result().getRows();
                        if (employees.isEmpty()) {
                            throw new Exception("Employee: not found");
                        }

                        JsonObject employee = employees.get(0);

                        Date date = UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(attendance.getString("register_at"), "");
                        String day = UtilsDate.getStringDayOfWeek(date);
                        JsonArray queryParams = new JsonArray()
                                .add(employee.getInteger("id"));
                        String query = QUERY_EMPLOYEE_SCHEDULE_BY_EMPLOYEE_ID + day + "=TRUE;";
                        conn.queryWithParams(query, queryParams, replySchedule -> {
                            try {
                                if(replySchedule.failed()) {
                                    throw new Exception(replySchedule.cause());
                                }

                                attendance.put("branchoffice_id", employee.getInteger("branchoffice_id"));
                                JsonArray scheduleList = new JsonArray(replySchedule.result().getRows());
                                JsonObject schedule = scheduleList.getJsonObject(0);

                                Date start = UtilsDate.parse_HH_mm(schedule.getString("hour_start"));
                                Date end = UtilsDate.parse_HH_mm(schedule.getString("hour_end"));
                                Date register = UtilsDate.parse_HH_mm(UtilsDate.format_HH_MM(date));

                                if(schedule.getBoolean("nights_watch")) {
                                    Calendar c = Calendar.getInstance();
                                    c.setTime(end);
                                    c.add(Calendar.DATE, 1);
                                    end = c.getTime();
                                }


                                long startDiff = start.getTime() - register.getTime();
                                long endDiff = register.getTime() - end.getTime();

                                attendance.put("start_diff", startDiff);
                                attendance.put("end_diff", endDiff);

                                long startDiffAbs = Math.abs(startDiff);
                                long endDiffAbs = Math.abs(endDiff);

                                if (startDiffAbs < endDiffAbs) {
                                    attendance.put("state", "in");
                                    attendance.put("state_diff", startDiffAbs);
                                } else {
                                    attendance.put("state", "out");
                                    attendance.put("state_diff", endDiffAbs);
                                }

                                System.out.println("start: ".concat(start.toString()));
                                System.out.println("start diff: ".concat(String.valueOf(startDiff)));
                                System.out.println("end: ".concat(end.toString()));
                                System.out.println("end diff: ".concat(String.valueOf(endDiff)));
                                System.out.println("register: ".concat(register.toString()));
                                System.out.println("state: ".concat(attendance.getString("state")));
                                System.out.println("===============================");

                                Date startLess30Minutes = new Date(start.getTime() - (1000 * 60 * 30));
                                Date endPlus3Hours = new Date(end.getTime() + (1000 * 60 * 60 * 3));


                                if(!(UtilsDate.isGreaterThanEqual(register, startLess30Minutes) && UtilsDate.isLowerThanEqual(register, endPlus3Hours))) {
                                    throw new Exception("register_at out of range");
                                }

                                conn.update(generateGenericCreate("employee_attendance", attendance), replyInsert -> {
                                    try {
                                        if (replyInsert.failed()) {
                                            throw new Exception(replyInsert.cause());
                                        }

                                        Integer id = replyInsert.result().getKeys().getInteger(0);
                                        this.commit(conn, message, new JsonObject().put("id", id));

                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                        this.rollback(conn, ex, message);
                                    }
                                });
                            }catch (Exception ex) {
                                ex.printStackTrace();
                                this.rollback(conn, ex, message);
                            }
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        this.rollback(conn, ex, message);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                reportQueryError(message, e);
            }
        });
    }

    private void driverAttendanceReport(Message<JsonObject> message) {
        try {
            String start_date =message.body().getString("start_date");
            String end_date =message.body().getString("end_date");
            JsonArray queryParams = new JsonArray()
                    .add(start_date)
                    .add(end_date);
            this.dbClient.queryWithParams(QUERY_TOTAL_TRACKING_HOURS_BY_DRIVER, queryParams,replyBusHours -> {
                try {
                    if (replyBusHours.failed()) {
                        throw new Exception(replyBusHours.cause());
                    }
                    JsonArray busDrivers = new JsonArray(replyBusHours.result().getRows());

                    this.dbClient.queryWithParams(QUERY_TOTAL_VAN_DRIVERS_HOUR, queryParams, replyVanHours -> {
                        try {
                            if (replyVanHours.failed()) {
                                throw new Exception(replyBusHours.cause());
                            }
                            JsonArray vanDrivers = new JsonArray(replyVanHours.result().getRows());
                            JsonArray drivers = new JsonArray();
                            for(int i = 0; i<busDrivers.size(); i++) {
                                drivers.add(busDrivers.getJsonObject(i));
                            }

                            for(int i = 0; i<vanDrivers.size(); i++) {
                                drivers.add(vanDrivers.getJsonObject(i));
                            }

                            message.reply(drivers);
                        }catch (Exception ex) {
                            ex.printStackTrace();
                            reportQueryError(message, ex);
                        }
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    reportQueryError(message, ex);
                }

            });
        } catch (Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private void getEmployees(Message<JsonObject> msg) {

        String queryFindEmployees = "SELECT * FROM employee;";

        this.dbClient.query(queryFindEmployees, replyEmployees -> {
            try {
                if (replyEmployees.failed()) {
                    throw new Exception(replyEmployees.cause());
                }

                msg.reply(new JsonArray(replyEmployees.result().getRows()));

            } catch (Exception ex) {
                ex.printStackTrace();
                reportQueryError(msg, ex);
            }
        });
    }

    private Future<Date> getNextAttendanceDayDate(JsonObject employee) {
        Future<Date> future = Future.future();

        try {
            Date today = new Date();
            Integer ID = employee.getInteger("id");
            Integer attendanceDayID = employee.getInteger("employee_attendance_day_id");
            if (attendanceDayID != null) {
                String query = "SELECT * FROM employee_attendance_day WHERE id = ?";
                JsonArray params = new JsonArray().add(attendanceDayID);

                // System.out.println(query.concat(params.encodePrettily()));
                this.dbClient.queryWithParams(query, params, reply -> {
                   try {
                       if (reply.failed()){
                           throw new Exception(reply.cause());
                       }

                       JsonObject attendance = reply.result().getRows().get(0);
                       Date date = UtilsDate.parse_yyyy_MM_dd(attendance.getString("attendance_day"));
                       Date nextDate = new Date(date.getTime() + 1000 * 60 * 60 * 24);

                       if (nextDate.after(today)) {
                           future.complete();
                       } else {
                           System.out.println("Found last attendance: ".concat(attendance.encodePrettily()));
                           System.out.println("Next date: ".concat(nextDate.toString()));
                           future.complete(nextDate);
                       }

                   } catch(Exception ex) {
                       ex.printStackTrace();
                       future.fail(ex);
                   }
                });

            } else {
                String query = "SELECT * FROM employee_attendance WHERE employee_id = ? ORDER BY register_at LIMIT 1";
                JsonArray params = new JsonArray().add(ID);

                // System.out.println(query.concat(params.encodePrettily()));
                this.dbClient.queryWithParams(query, params, reply -> {
                    try {
                        if (reply.failed()){
                            throw new Exception(reply.cause());
                        }

                        List<JsonObject> attendances = reply.result().getRows();
                        if (attendances.isEmpty()) {
                            future.complete();
                        } else {
                            JsonObject attendance = attendances.get(0);
                            Date date = UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(attendance.getString("register_at"), "UTC");
                            if (date.after(today)) {
                                future.complete();
                            } else {
                                future.complete(date);
                            }
                        }
                    } catch(Exception ex) {
                        ex.printStackTrace();
                        future.fail(ex);
                    }
                });

            }

        } catch (Exception ex) {
            ex.printStackTrace();
            future.fail(ex);
        }

        return future;
    }

    private void aggregateAttendanceDay(Message<JsonObject> msg) {
        this.startTransaction(msg, conn -> {
            try {
                JsonObject body = msg.body();
                Integer employeeID = body.getInteger("id");

                getNextAttendanceDayDate(body).setHandler(replyDate -> {
                    try {
                        if (replyDate.failed()) {
                            throw new Exception(replyDate.cause());
                        }

                        Date date = replyDate.result();
                        if (date == null) {
                            this.commit(conn, msg, new JsonObject());
                            return;
                        }
                        String strDate = UtilsDate.format_yyyy_MM_dd(date);

                        String queryFindAttendance = "SELECT * FROM employee_attendance " +
                                "WHERE employee_id = ? AND DATE(register_at) = ? AND state = ? " +
                                "ORDER BY state_diff LIMIT 1";

                        JsonArray findAttendanceParams = new JsonArray()
                                .add(employeeID)
                                .add(strDate);

                        System.out.println("Updating attendance: ".concat(findAttendanceParams.encodePrettily()));

                        // Find IN for employee and day
                        conn.queryWithParams(queryFindAttendance, findAttendanceParams.copy().add("in"), replyFindIn -> {
                            try {
                                if (replyFindIn.failed()) {
                                    throw new Exception(replyFindIn.cause());
                                }

                                List<JsonObject> ins = replyFindIn.result().getRows();

                                // Find OUT for employee and day
                                conn.queryWithParams(queryFindAttendance, findAttendanceParams.copy().add("out"), replyFindOut -> {
                                    try {
                                        if (replyFindOut.failed()) {
                                            throw new Exception(replyFindOut.cause());
                                        }

                                        List<JsonObject> outs = replyFindOut.result().getRows();

                                        JsonObject attendanceDay = new JsonObject()
                                                .put("created_by", 0)
                                                .put("employee_id", employeeID)
                                                .put("attendance_day", UtilsDate.format_yyyy_MM_dd(date));

                                        // If there is no register set as absence
                                        if (ins.isEmpty()) {
                                            attendanceDay.put("attendance_status", "absence");
                                            if (!outs.isEmpty()) {
                                                JsonObject out = outs.get(0);
                                                attendanceDay.put("employee_attendance_out_id", out.getValue("id"));
                                            }
                                        } else {
                                            JsonObject in = ins.get(0);
                                            long startDiff = in.getLong("start_diff");
                                            attendanceDay.put("employee_attendance_in_id", in.getValue("id"));

                                            // If start diff is mayor as 30 minutes (in ms) (positive means late)
                                            long minutesAllowedToArriveLate = 1000 * 60 * 30;
                                            if (startDiff > 0 && minutesAllowedToArriveLate < startDiff) {
                                                attendanceDay.put("attendance_status", "late");
                                            }

                                            if (!outs.isEmpty()) {
                                                JsonObject out = outs.get(0);
                                                attendanceDay.put("employee_attendance_out_id", out.getValue("id"));

                                                // Calculate hours
                                                Date inRegisterAt = UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(in.getString("register_at"), "UTC");
                                                Date outRegisterAt = UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(out.getString("register_at"), "UTC");

                                                Period p = new Period(inRegisterAt.getTime(), outRegisterAt.getTime());
                                                int hours = p.getHours();
                                                int minutes = p.getMinutes();
                                                String format = String.format("%%0%dd", 2);
                                                attendanceDay.put("hours", String.format(format, hours).concat(":")
                                                        .concat(String.format(format, minutes)).concat(":00"));
                                            }
                                        }

                                        String insert = this.generateGenericCreate("employee_attendance_day", attendanceDay);
                                        conn.update(insert, replyInsert -> {
                                            try {
                                                if (replyInsert.failed()) {
                                                    throw new Exception(replyInsert.cause());
                                                }

                                                Integer id = replyInsert.result().getKeys().getInteger(0);
                                                attendanceDay.put("id", id);

                                                String queryUpdateEmployee = "UPDATE employee SET employee_attendance_day_id = ? WHERE id = ?";
                                                JsonArray paramsUpdateEmployee = new JsonArray().add(id).add(employeeID);
                                                conn.updateWithParams(queryUpdateEmployee, paramsUpdateEmployee, replyUpdateEmployee -> {
                                                    try {
                                                        if (replyUpdateEmployee.failed()) {
                                                            throw new Exception(replyUpdateEmployee.cause());
                                                        }

                                                        System.out.println("attendance: ".concat(attendanceDay.encodePrettily()));
                                                        this.commit(conn, msg, new JsonObject());

                                                    } catch (Exception ex) {
                                                        ex.printStackTrace();
                                                        this.rollback(conn, ex, msg);
                                                    }
                                                });

                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                                this.rollback(conn, ex, msg);
                                            }
                                        });

                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                        this.rollback(conn, ex, msg);
                                    }

                                });

                            } catch (Exception ex) {
                                ex.printStackTrace();
                                this.rollback(conn, ex, msg);
                            }

                        });

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        this.rollback(conn, ex, msg);
                    }
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                this.rollback(conn, ex, msg);
            }
        });

    }

    private void searchEmployeeByName(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String searchTerm = body.getString("searchTerm");
            Integer jobId = body.getInteger("job_id");
            Integer branchofficeId = body.getInteger(_BRANCHOFFICE_ID);
            Integer limit = body.getInteger("limit", 0);

            String QUERY = QUERY_SEARCH_ADVANCED;
            JsonArray param = new JsonArray();
            int customerId = 0;
            try {
                customerId = Integer.parseInt(body.getString("searchTerm"));
            } catch (Throwable t) {
                searchTerm = "%" + body.getString("searchTerm") + "%";
            }

            if (customerId == 0) {
                QUERY = QUERY.concat(SEARCH_TERM_FILTER_MULTIPLE);
                searchTerm = searchTerm.replace(' ', '%');
                param.add(searchTerm);
            } else {
                QUERY = QUERY.concat(SEARCH_ID_FILTER);
                param.add(customerId);
            }

            if (Objects.nonNull(jobId)) {
                QUERY = QUERY.concat(SEARCH_JOB_FILTER);
                param.add(jobId);
            }

            if (Objects.nonNull(branchofficeId)) {
                QUERY = QUERY.concat(SEARCH_BRANCHOFFICE_FILTER);
                param.add(branchofficeId);
            }

            QUERY = QUERY.concat(SEARCH_ORDER_BY);
            if (limit > 0) QUERY += "LIMIT " + limit;
            dbClient.queryWithParams(QUERY, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> resultCustomers = reply.result().getRows();
                    if (resultCustomers.isEmpty()) {
                        message.reply(new JsonArray());
                    } else {
                        message.reply(new JsonArray(resultCustomers));
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    reportQueryError(message, ex);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }
    private void getEmployeesOperator(Message<JsonObject> message) {
        int profile_id = 0;
        int id_branch = 0;
        String  query="";
        try {
            profile_id = Integer.parseInt(message.body().getString("profile_id"));
            id_branch = Integer.parseInt(message.body().getString("id_branch"));
            query= GET_ALL_OPERATOR_CP+profile_id+" and e.branchoffice_id="+id_branch;
        } catch (Throwable t) {
            query=GET_ALL_OPERATOR_CP+3;
        }
        this.dbClient.query(query, replyEmployees -> {
            try {
                if (replyEmployees.failed()) {
                    throw new Exception(replyEmployees.cause());
                }

                message.reply(new JsonArray(replyEmployees.result().getRows()));

            } catch (Exception ex) {
                ex.printStackTrace();
                reportQueryError(message, ex);
            }
        });
    }

    private static final String QUERY_AVALAIBLE_EMPLOYEES = "SELECT \n"
            + " emp.id, CONCAT_WS(\" \", emp.name, emp.last_name) AS full_name, emp.job_id, j.name AS job_name\n"
            + " FROM employee AS emp \n"
            + " INNER JOIN job j ON j.id = emp.job_id \n"
            + " WHERE NOT EXISTS ( \n"
            + "   SElECT srd.schedule_route_id FROM schedule_route_driver AS srd \n"
            + "   INNER JOIN schedule_route AS sr ON sr.id=srd.schedule_route_id \n"
            + "   WHERE srd.employee_id=emp.id AND sr.status=1 AND sr.schedule_status = 'scheduled'\n"
            + "   AND srd.status = 1 AND srd.driver_status = 1"
            + " ) \n"
            + " AND emp.status = 1 AND emp.job_id = ?;";

    private static final String QUERY_AVALAIBLE_VAN_DRIVERS = "SELECT e.id, e.name, e.last_name, e.birthday, e.avatar_file, e.start_working_at\n" +
            "FROM employee AS e \n" +
            "JOIN branchoffice AS b \n" +
            "WHERE e.branchoffice_id = b.id AND b.city_id = ? AND \n" +
            "e.job_id = ? AND e.status = 1 AND \n" +
            "e.id NOT IN (SELECT rd.employee_id \n" +
            "FROM rental AS r \n" +
            "LEFT JOIN rental_driver AS rd ON rd.rental_id = r.id \n" +
            "WHERE r.rent_status = 2 AND r.has_driver = 1 AND r.status = 1 AND rd.status = 1);";

    private static final String QUERY_EMPLOYEE_BY_USER_ID = "SELECT emp.*\n"
        + "FROM employee AS emp WHERE emp.user_id = ? LIMIT 1";

    private  static final String UPDATE_EMPLOYEE_WITH_FINGER_PRINT="UPDATE employee SET finger_print = ? WHERE user_id = ? ;";

    private static final String QUERY_EMPLOYEE_BY_ID = "SELECT * FROM employee WHERE id = ?;";

    private static final String QUERY_EMPLOYEE_SCHEDULE_BY_EMPLOYEE_ID = "SELECT * FROM employee_schedule WHERE employee_id = ? AND status = 1 AND ";

    private static final String QUERY_TOTAL_TRACKING_HOURS_BY_DRIVER = "SELECT \n"
            + "SUM(TIME_TO_SEC(dt.time_tracking)) AS hours,\n"
            + "e.name, e.last_name, e.curp, e.nss\n"
            + "FROM driver_tracking AS dt\n"
            + "INNER JOIN employee AS e ON e.id = dt.employee_id\n"
            + "WHERE dt.created_at BETWEEN ? AND ?\n"
            + "GROUP BY dt.employee_id;";

    private static final String QUERY_TOTAL_VAN_DRIVERS_HOUR = "SELECT\n" +
            "e.name, e.last_name, e.curp, e.nss,\n" +
            "SUM(TIME_TO_SEC(TIMEDIFF(r.received_at, r.delivered_at))) AS hours\n" +
            "FROM rental AS r\n" +
            "INNER JOIN rental_driver AS rd ON rd.rental_id = r.id\n" +
            "INNER JOIN employee AS e ON rd.employee_id = e.id\n" +
            "WHERE r.received_at BETWEEN ? AND ? \n" +
            "AND r.is_quotation = FALSE\n" +
            "AND r.has_driver = TRUE\n" +
            "GROUP BY rd.employee_id\n" +
            ";";
    private static final String QUER_GET_VALUE_DRIVER_BUS = "SELECT value FROM general_setting WHERE FIELD = 'driver_bus_id';";

    private static final String QUERY_UPDATE_DELETED_EMPLOYEE = "update employee e\n" +
            "left join users u on u.id = e.user_id\n" +
            "SET e.email = CONCAT(e.email, 'DELETED', e.id), u.email = IF(u.id IS NOT NULL, CONCAT(u.email, 'DELETED', u.id), u.email)\n" +
            "where e.email = ? AND e.status = 3";


    private static final String QUERY_SEARCH_ADVANCED = "select \n" +
            "u.id,\n" +
            "u.name ,\n" +
            "u.phone,\n" +
            "u.email,\n" +
            "u.status\n" +
            "from users u \n" +
            "INNER JOIN employee e ON e.user_id = u.id AND e.status = 1\n" +
            "INNER JOIN job j ON j.id = e.job_id AND j.status = 1\n" +
            "WHERE u.status != 3 \n";

    private static final String SEARCH_TERM_FILTER = " AND CONCAT_WS(' ' , em.name , em.phone, em.email ) LIKE ?";

    private static final String SEARCH_TERM_FILTER_MULTIPLE = " AND concat_ws(' ' , u.name , u.phone , u.email ) LIKE ? AND u.profile_id  in ( select id from profile )  AND u.profile_id is not null ";
    private static final String SEARCH_ID_FILTER = " AND u.id = ? AND u.profile_id  in ( select id from profile ) AND u.profile_id is not null \n";
    private static final String SEARCH_JOB_FILTER = " AND j.id = ? \n";
    private static final String SEARCH_BRANCHOFFICE_FILTER = " AND e.branchoffice_id = ? \n";
    private static final String SEARCH_ORDER_BY = " ORDER BY u.name\n";
    private static final String GET_ALL_OPERATOR_CP = "SELECT  u.id, u.name,u.email,e.driver_license, e.rfc FROM users as u inner join employee as e on e.user_id=u.id  where  u.status=1 and u.profile_id=";
}
