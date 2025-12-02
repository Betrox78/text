package database.parcel.handlers.ParcelsManifestDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.money.CashOutDBV;
import database.parcel.ParcelsManifestDBV;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import utils.UtilsDate;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.money.CashOutDBV.ACTION_OPEN_CASH_OUT;
import static service.commons.Constants.*;

public class Register extends DBHandler<ParcelsManifestDBV> {

    public Register(ParcelsManifestDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer branchofficeId = body.getInteger(BRANCHOFFICE_ID);
            Integer serviceTypeId = body.getInteger(_TYPE_SERVICE_ID);
            Integer createdBy = body.getInteger(CREATED_BY);

            JsonObject cashOut = body.getJsonObject(_CASH_OUT);
            Integer employeeId = cashOut.getInteger(EMPLOYEE_ID);
            Integer vehicleId = cashOut.getInteger(_VEHICLE_ID);

            String serviceType = serviceTypeId.equals(2) ? "EAD" : "RAD";

            Future<Boolean> f1 = Future.future();
            Future<JsonObject> f2 = Future.future();
            Future<String> f3 = Future.future();
            checkParcelManifestOpen(employeeId).setHandler(f1.completer());
            getVehicleRadEadInfo(employeeId, vehicleId, branchofficeId).setHandler(f2.completer());
            getFolio(serviceType, branchofficeId).setHandler(f3.completer());

            CompositeFuture.all(f1, f2, f3).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    JsonObject vehicleRadEadInfo = reply.result().resultAt(1);
                    String folio = reply.result().resultAt(2);

                    startTransaction(message, conn -> {
                        try {
                            openCashoutDriver(cashOut, branchofficeId, createdBy).whenComplete((cashOutId, errCO) -> {
                                try {
                                    if (errCO != null) {
                                        throw errCO;
                                    }

                                    createParcelManifest(conn, folio, serviceTypeId, branchofficeId, vehicleRadEadInfo, cashOutId, createdBy).whenComplete((parcelManifestId, errPM) -> {
                                        try {
                                            if (errPM != null) {
                                                throw errPM;
                                            }

                                            this.commit(conn, message, new JsonObject()
                                                    .put(_PARCEL_MANIFEST_ID, parcelManifestId)
                                                    .put(CASHOUT_ID, cashOutId)
                                                    .put(_VEHICLE_ID, vehicleRadEadInfo.getInteger(_VEHICLE_ID))
                                                    .put(_VEHICLE_NAME, vehicleRadEadInfo.getString(_VEHICLE_NAME)));
                                        } catch (Throwable t) {
                                            this.rollback(conn, t, message);
                                        }
                                    });
                                } catch (Throwable t) {
                                    this.rollback(conn, t, message);
                                }
                            });
                        } catch (Throwable t) {
                            this.rollback(conn, t, message);
                        }
                    });
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private Future<Boolean> checkParcelManifestOpen(int employeeId) {
        Future<Boolean> future = Future.future();
        try {
            JsonArray param = new JsonArray()
                    .add(employeeId);
            this.dbClient.queryWithParams(QUERY_GET_PARCEL_MANIFES_OPEN_TODAY, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if (!results.isEmpty()) {
                        throw new Exception("The employee has an open manifest today");
                    }

                    future.complete(true);
                } catch (Throwable t) {
                    future.fail(t);
                }
            });
        } catch (Throwable t) {
            future.fail(t);
        }
        return future;
    }

    private Future<JsonObject> getVehicleRadEadInfo(int employeeId, int vehicleId, int branchofficeId) {
        Future<JsonObject> future = Future.future();
        try {
            JsonArray param = new JsonArray().add(vehicleId).add(branchofficeId);
            this.dbClient.queryWithParams(QUERY_VALIDATE_VEHICLE_RAD_EAD, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if (results.isEmpty()) {
                        throw new Exception("Vehicle is not assigned to branchoffice");
                    }

                    List<JsonObject> filteredVehicles = results.stream()
                            .filter(v -> v.getInteger("id_employee").equals(employeeId))
                            .collect(Collectors.toList());

                    if (filteredVehicles.isEmpty()) {
                        throw new Exception("Vehicle is not assigned to the employee");
                    }

                    future.complete(filteredVehicles.get(0));
                } catch (Throwable t) {
                    future.fail(t);
                }
            });
        } catch (Throwable t) {
            future.fail(t);
        }
        return future;
    }

    private Future<String> getFolio(String serviceType, Integer branchofficeId) {
        Future<String> future = Future.future();
        try {
            JsonArray param = new JsonArray().add(branchofficeId);
            this.dbClient.queryWithParams(QUERY_GET_LAST_FOLIO_BY_BRANCHOFFICE, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if (results.isEmpty()) {
                        throw new Exception("Branchoffice not found");
                    }

                    String prefix = results.get(0).getString("prefix");
                    String lastFolio = results.get(0).getString("folio");
                    String newFolio = serviceType.concat(prefix);
                    if (Objects.isNull(lastFolio)) {
                        newFolio = newFolio.concat("1");
                    } else {
                        int nextValue = Integer.parseInt(lastFolio.substring(3 + prefix.length())) + 1;
                        newFolio = newFolio.concat(String.valueOf(nextValue));
                    }

                    future.complete(newFolio);
                } catch (Throwable t) {
                    future.fail(t);
                }
            });
        } catch (Throwable t) {
            future.fail(t);
        }
        return future;
    }

    private CompletableFuture<Integer> createParcelManifest(SQLConnection conn, String folio, int typeServiceId, int branchofficeId, JsonObject vehicleRadEadInfo, int cashOutId, int createdBy) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            GenericQuery create = this.generateGenericCreate("parcels_manifest", new JsonObject()
                    .put("folio", folio)
                    .put("id_type_service", typeServiceId)
                    .put("num_route", vehicleRadEadInfo.getInteger("num_route"))
                    .put("id_vehicle_rad_ead", vehicleRadEadInfo.getInteger(ID))
                    .put("drive_name", vehicleRadEadInfo.getString("drive_name"))
                    .put("vehicle_serial_num", vehicleRadEadInfo.getString("vehicle_serial_num"))
                    .put("id_branchoffice", branchofficeId)
                    .put(CASHOUT_ID, cashOutId)
                    .put("init_load_date", UtilsDate.sdfDataBase(new Date()))
                    .put(STATUS, 1)
                    .put(CREATED_BY, createdBy));

            conn.updateWithParams(create.getQuery(), create.getParams(), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    final int id = reply.result().getKeys().getInteger(0);
                    future.complete(id);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Integer> openCashoutDriver(JsonObject cashOutBody, int branchofficeId, int createdBy) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            Integer cashRegisterId = cashOutBody.getInteger(CASH_REGISTER_ID);
            checkCashRegister(cashRegisterId, branchofficeId).whenComplete((replyCCR, errCCR) -> {
                try {
                    if (errCCR != null) {
                        throw errCCR;
                    }

                    cashOutBody.put(CREATED_BY, createdBy);
                    this.getVertx().eventBus().send(CashOutDBV.class.getSimpleName(), cashOutBody,
                            new DeliveryOptions().addHeader(ACTION, ACTION_OPEN_CASH_OUT), reply -> {
                                try {
                                    if (reply.failed()){
                                        throw reply.cause();
                                    }
                                    JsonObject response = (JsonObject) reply.result().body();
                                    future.complete(response.getInteger(ID));

                                } catch(Throwable t){
                                    future.completeExceptionally(t);
                                }
                            });
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> checkCashRegister(int cashRegisterId, int branchofficeId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_CHECK_CASH_REGISTER, new JsonArray().add(cashRegisterId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> results = reply.result().getRows();
                if (results.isEmpty()) {
                    throw new Exception("Cash register not found");
                }

                JsonObject cashRegister = results.get(0);
                Integer cashRegisterBranchofficeId = cashRegister.getInteger(BRANCHOFFICE_ID);
                if (!cashRegisterBranchofficeId.equals(branchofficeId)) {
                    throw new Exception("The cash register does not belong to this branchoffice");
                }

                Integer cashRegisterStatus = cashRegister.getInteger(STATUS);
                Boolean cashRegisterCOStatus = cashRegister.getBoolean("cash_out_status");
                if (!cashRegisterStatus.equals(1) || cashRegisterCOStatus) {
                    throw new Exception("The cash register is not available");
                }

                future.complete(true);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private static final String QUERY_GET_PARCEL_MANIFES_OPEN_TODAY = "SELECT\n" +
            "   pm.*\n" +
            "FROM parcels_manifest pm\n" +
            "WHERE pm.status = 1\n" +
            "   AND pm.created_by = ?\n";

    private static final String QUERY_VALIDATE_VEHICLE_RAD_EAD = "SELECT \n" +
            "   vre.*,\n" +
            "   CONCAT(e.name, ' ', e.last_name) AS drive_name,\n" +
            "   v.serial_num AS vehicle_serial_num,\n" +
            "   v.id AS vehicle_id,\n" +
            "   v.name AS vehicle_name," +
            "   (SELECT\n" +
            "       COUNT(pm.id) + 1\n" +
            "   FROM parcels_manifest pm\n" +
            "   WHERE pm.id_branchoffice = vre.id_branchoffice\n" +
            "   AND DATE(CONVERT_TZ(pm.created_at, '+00:00', '" + UtilsDate.getTimeZoneValue() + "'))\n" +
            "    = DATE(CONVERT_TZ(NOW(), '+00:00', '" + UtilsDate.getTimeZoneValue() + "'))) AS num_route\n" +
            "FROM vehicle_rad_ead vre\n" +
            "INNER JOIN employee e ON e.id = vre.id_employee\n" +
            "INNER JOIN vehicle v ON v.id = vre.id_vehicle\n" +
            "INNER JOIN branchoffice b ON b.id = vre.id_branchoffice\n" +
            "WHERE vre.status = 1\n" +
            "   AND vre.id_vehicle = ?\n" +
            "   AND vre.id_branchoffice = ?";

    private static final String QUERY_GET_LAST_FOLIO_BY_BRANCHOFFICE = "SELECT \n" +
            "   b.prefix,\n" +
            "   pm.folio \n" +
            "FROM parcels_manifest pm \n" +
            "INNER JOIN branchoffice b ON b.id = pm.id_branchoffice\n" +
            "WHERE pm.id_branchoffice = ?\n" +
            "ORDER BY pm.id DESC LIMIT 1;";

    private static final String QUERY_CHECK_CASH_REGISTER = "SELECT * FROM cash_registers WHERE id = ?;";
}
