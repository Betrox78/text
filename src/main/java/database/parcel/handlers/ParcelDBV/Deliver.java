package database.parcel.handlers.ParcelDBV;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.money.PaybackDBV;
import database.money.PaymentDBV;
import database.parcel.ParcelDBV;
import database.parcel.ParcelsPackagesDBV;
import database.parcel.ParcelsPackagesTrackingDBV;
import database.parcel.enums.PARCEL_MANIFEST_DETAIL_STATUS;
import database.parcel.enums.PARCEL_MANIFEST_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import org.jetbrains.annotations.NotNull;
import service.commons.Constants;
import utils.UtilsDate;
import utils.UtilsID;
import utils.UtilsMoney;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static service.commons.Constants.*;
import static utils.UtilsDate.sdfDataBase;

public class Deliver extends DBHandler<ParcelDBV> {

    public Deliver(ParcelDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject deliverBody = message.body();
            JsonObject parcel = deliverBody.getJsonObject("parcel");
            JsonObject employee = (JsonObject) deliverBody.remove(EMPLOYEE);
            JsonArray parcelPackages = deliverBody.getJsonArray("parcel_packages");
            final double ivaPercent = message.body().getDouble("iva_percent");
            final int currencyId = message.body().getInteger("currency_id");
            JsonArray payments = (JsonArray) deliverBody.remove("payments");
            JsonObject cashChange = (JsonObject) deliverBody.remove("cash_change");
            Integer parcelId = deliverBody.getInteger("id");
            Integer cashOutId = deliverBody.getInteger(CASHOUT_ID);
            Integer updatedBy = deliverBody.getInteger("updated_by");
            final Boolean is_credit = deliverBody.containsKey("is_credit") ? deliverBody.getBoolean("is_credit") : Boolean.valueOf(false);

            //change this , its a test to the rad parcel WEBDEV
            JsonObject rad = new JsonObject();

            int numPackages = parcelPackages.size();
            List<CompletableFuture<JsonObject>> packagesTasks = new ArrayList<>();
            List<String> packagesIds = new ArrayList<>();

            this.startTransaction(message, (SQLConnection conn) -> {
                try {
                    PARCEL_STATUS parcelStatus = PARCEL_STATUS.values()[parcel.getInteger("parcel_status")];
                    if (!parcelStatus.canDeliver()) {
                        throw new Exception("Parcel was delivered or canceled.");
                    }

                    this.createParcelDelivery(conn, deliverBody).whenComplete((parcelDeliveriesId, errorParcelsDeliveries) ->{
                        try{
                            if(errorParcelsDeliveries != null){
                                throw errorParcelsDeliveries;
                            }

                            for (int i = 0; i < numPackages; i++) {
                                JsonObject parcelPackage = parcelPackages.getJsonObject(i);
                                parcelPackage.put("parcel_id", parcelId)
                                        .put("terminal_id", employee.getInteger("branchoffice_id"));
                                int packageStatus = PARCEL_STATUS.DELIVERED_OK.ordinal();
                                JsonArray incidences = parcelPackage.getJsonArray("packages_incidences");
                                if(incidences != null && !incidences.isEmpty()){
                                    packageStatus = PARCEL_STATUS.DELIVERED_WITH_INCIDENCES.ordinal();
                                }
                                packagesIds.add(parcelPackage.getInteger("id").toString());
                                parcelPackage.put("package_status", packageStatus);
                                packagesTasks.add(deliverPackage(conn, parcelPackage, updatedBy, parcelId, parcelDeliveriesId, employee.getInteger("branchoffice_id")));
                            }
                            deliverBody.remove("credential_type");
                            deliverBody.remove("no_credential");
                            double innerTotalAmount = UtilsMoney.round(parcel.getDouble("total_amount"), 2);

                            CompletableFuture.allOf(packagesTasks.toArray(new CompletableFuture[numPackages])).whenComplete((ps, pt) -> {
                                try{
                                    if (pt != null) {
                                        throw pt;
                                    }

                                    this.insertTracking(conn, parcelPackages,
                                            "parcels_packages_tracking", "parcel_id", "parcel_package_id", "Entrega de paquete",
                                            ParcelsPackagesTrackingDBV.ACTION.DELIVERED.name().toLowerCase(), updatedBy).whenComplete((resultInsertTracking, errorInsertTracking) -> {
                                        try {
                                            if (errorInsertTracking != null){
                                                throw new Exception(errorInsertTracking);
                                            }
                                            boolean paysSender = parcel.getBoolean("pays_sender");
                                            final double finalTotalAmount = innerTotalAmount;

                                            if (!paysSender && !parcelStatus.equals(PARCEL_STATUS.DELIVERED_PARTIAL)) {
                                                conn.queryWithParams("SELECT * FROM parcels_packages WHERE id IN (?)", new JsonArray().add(String.join(",", packagesIds)),replyPackage -> {
                                                    try{
                                                        if (replyPackage.failed()){
                                                            throw replyPackage.cause();
                                                        }

                                                        List<JsonObject> packages = replyPackage.result().getRows();
                                                        JsonArray packagesArray = new JsonArray(packages);

                                                        if(!is_credit) {
                                                            paymentsValidations(payments, innerTotalAmount);
                                                        }

                                                        String parcelAction = is_credit ? "voucher" : "purchase";
                                                        this.insertTicket(conn, cashOutId, parcelId, finalTotalAmount, cashChange, updatedBy, ivaPercent, parcelAction).whenComplete((JsonObject ticket, Throwable ticketError) -> {
                                                            try{
                                                                if (ticketError != null) {
                                                                    throw ticketError;
                                                                }
                                                                deliverBody.put("ticket_id", ticket.getInteger("id"));
                                                                // Insert ticket detail
                                                                this.insertTicketDetail(conn, ticket.getInteger("id"), updatedBy, packagesArray, new JsonArray(), parcel, rad).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                                                                    try{
                                                                        if (dError != null) {
                                                                            throw dError;
                                                                        }

                                                                        // insert payments
                                                                        List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                                                                        for (int i = 0; i < payments.size(); i++) {
                                                                            JsonObject payment = payments.getJsonObject(i);
                                                                            payment.put("ticket_id", ticket.getInteger("id"));
                                                                            pTasks.add(insertPaymentAndCashOutMove(conn, payment, currencyId, parcel.getInteger("id"), cashOutId, updatedBy, is_credit));
                                                                        }

                                                                        CompletableFuture<Void> allPayments = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[payments.size()]));
                                                                        allPayments.whenComplete((s, tt) -> {
                                                                            try{
                                                                                if (tt != null) {
                                                                                    throw tt;
                                                                                }

                                                                                int terminalOriginId = parcel.getInteger(_TERMINAL_ORIGIN_ID);
                                                                                int terminalDestinyId = parcel.getInteger(_TERMINAL_DESTINY_ID);
                                                                                getDistanceBtwTerminals(terminalOriginId, terminalDestinyId).whenComplete((distanceKm, errDKM) -> {
                                                                                    try {
                                                                                        if (errDKM != null){
                                                                                            throw errDKM;
                                                                                        }

                                                                                        PaybackDBV objPayback = new PaybackDBV();
                                                                                        objPayback.calculatePointsParcel(conn, distanceKm, numPackages, false).whenComplete((resultCalculate, error) ->{
                                                                                            try {
                                                                                                if (error != null) {
                                                                                                    throw error;
                                                                                                }

                                                                                                Double paybackMoney = resultCalculate.getDouble("money");
                                                                                                Double paybackPoints = resultCalculate.getDouble("points");

                                                                                                JsonObject paramMovPayback = new JsonObject()
                                                                                                        .put("customer_id", parcel.getInteger(Constants.CUSTOMER_ID))
                                                                                                        .put("points", paybackPoints)
                                                                                                        .put("money", paybackMoney)
                                                                                                        .put("type_movement", "I")
                                                                                                        .put("motive", "Envío de paquetería(addresse)")
                                                                                                        .put("id_parent", parcelId)
                                                                                                        .put("employee_id", updatedBy);
                                                                                                objPayback.generateMovementPayback(conn, paramMovPayback).whenComplete((movementPayback, errorMP) ->{
                                                                                                    try {
                                                                                                        if (errorMP != null) {
                                                                                                            throw errorMP;
                                                                                                        }

                                                                                                        List<Future> checks = new ArrayList<>();
                                                                                                        checks.add(checkPendants(conn, parcelId));
                                                                                                        checks.add(checkIncidences(conn, parcelId));

                                                                                                        CompositeFuture.all(checks).setHandler(reply -> {
                                                                                                            try {
                                                                                                                if (reply.failed()) {
                                                                                                                    throw reply.cause();
                                                                                                                }
                                                                                                                int pendantsPackages = reply.result().resultAt(0);
                                                                                                                int incidences = reply.result().resultAt(1);

                                                                                                                PARCEL_STATUS parcelStatusDeliver = getParcelDeliverStatus(pendantsPackages, incidences);

                                                                                                                this.updateParcelStatus(conn, parcelId, parcelStatusDeliver.ordinal(), paybackMoney, is_credit, updatedBy).whenComplete((pp, ttt) -> {
                                                                                                                    try{
                                                                                                                        if (ttt != null) {
                                                                                                                            throw ttt;
                                                                                                                        }
                                                                                                                        this.commit(conn, message, deliverBody);
                                                                                                                    } catch (Throwable t){
                                                                                                                        this.rollback(conn, t, message);
                                                                                                                    }
                                                                                                                });

                                                                                                            } catch (
                                                                                                                    Throwable t) {
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
                                                                                        this.rollback(conn, t, message);
                                                                                    }
                                                                                });
                                                                            } catch(Throwable t){
                                                                                this.rollback(conn, t, message);
                                                                            }
                                                                        });
                                                                    } catch(Throwable t){
                                                                        this.rollback(conn, t, message);
                                                                    }
                                                                });
                                                            } catch(Throwable t){
                                                                this.rollback(conn, t, message);
                                                            }
                                                        });
                                                    } catch(Throwable t){
                                                        this.rollback(conn, t, message);
                                                    }
                                                });

                                            } else {

                                                List<Future> checks = new ArrayList<>();
                                                checks.add(checkPendants(conn, parcelId));
                                                checks.add(checkIncidences(conn, parcelId));

                                                CompositeFuture.all(checks).setHandler(reply -> {
                                                    try {
                                                        if (reply.failed()) {
                                                            throw reply.cause();
                                                        }
                                                        int pendantsPackages = reply.result().resultAt(0);
                                                        int incidences = reply.result().resultAt(1);

                                                        PARCEL_STATUS parcelStatusDeliver = getParcelDeliverStatus(pendantsPackages, incidences);

                                                        this.updateParcelStatus(conn, parcelId, parcelStatusDeliver.ordinal(), null, is_credit, updatedBy).whenComplete((pp, ttt) -> {
                                                            try{
                                                                if (ttt != null) {
                                                                    throw ttt;
                                                                }
                                                                this.insertTicket(conn, cashOutId, parcelId, 0.00, null, updatedBy, 0.00,  "voucher").whenComplete((JsonObject ticketV, Throwable ticketErrorV) -> {
                                                                    try {
                                                                        if (ticketErrorV != null) {
                                                                            throw ticketErrorV;
                                                                        }
                                                                        this.insertTicketDetail(conn, ticketV.getInteger("id"), updatedBy, new JsonArray(), new JsonArray(), parcel, rad).whenComplete((Boolean detailsSuccessV, Throwable dErrorV) -> {
                                                                            try {
                                                                                if (dErrorV != null) {
                                                                                    throw dErrorV;
                                                                                }
                                                                                deliverBody.put("voucher_ticket_id", ticketV.getInteger("id"));
                                                                                this.commit(conn, message, deliverBody);
                                                                            } catch (Throwable t){
                                                                                this.rollback(conn, t, message);
                                                                            }
                                                                        });
                                                                    } catch (Throwable t){
                                                                        this.rollback(conn, t, message);
                                                                    }
                                                                });
                                                            } catch (Throwable t){
                                                                this.rollback(conn, t, message);
                                                            }
                                                        });

                                                    } catch (
                                                            Throwable t) {
                                                        this.rollback(conn, t, message);
                                                    }
                                                });

                                            }
                                        } catch (Exception e){
                                            this.rollback(conn, e, message);
                                        }
                                    });
                                } catch(Throwable t){
                                    this.rollback(conn, t, message);
                                }
                            });
                        } catch(Throwable t){
                            this.rollback(conn, t, message);
                        }
                    });
                } catch (Exception ex) {
                    this.rollback(conn, ex, message);
                }

            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private void paymentsValidations(JsonArray payments, double innerTotalAmount) throws Exception {
        double totalPayments = 0.0;
        if(payments == null) {
            throw new Exception("Payment not found");
        }

        for (int i = 0; i < payments.size(); i++) {
            JsonObject payment = payments.getJsonObject(i);
            Double paymentAmount = payment.getDouble("amount");
            if (paymentAmount == null || paymentAmount < 0.0) {
                throw new Exception("Invalid payment amount: " + paymentAmount);
            }
            totalPayments += UtilsMoney.round(paymentAmount, 2);
        }
        if (totalPayments > innerTotalAmount) {
            throw new Exception("The payment " + totalPayments + " is greater than the total " + innerTotalAmount);
        }
        if (totalPayments < innerTotalAmount) {
            throw new Exception("The payment " + totalPayments + " is lower than the total " + innerTotalAmount);
        }
    }

    private CompletableFuture<Double> getDistanceBtwTerminals(Integer terminalOriginId, Integer terminalDestinyId) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        JsonArray params = new JsonArray()
                .add(terminalOriginId)
                .add(terminalDestinyId)
                .add(terminalDestinyId)
                .add(terminalOriginId);
        dbClient.queryWithParams(ParcelsPackagesDBV.QUERY_DISTANCE_KM_BY_TERMINALS_ID, params, replyKm -> {
            try {
                if (replyKm.failed()) {
                    throw replyKm.cause();
                }
                if (replyKm.result().getNumRows() == 0) {
                    throw new Exception("Distance km not found");
                }
                Double paramKm = replyKm.result().getRows().get(0).getDouble("distance_km");
                future.complete(paramKm);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @NotNull
    private static PARCEL_STATUS getParcelDeliverStatus(int pendantsPackages, int incidences) {
        PARCEL_STATUS parcelStatusDeliver;
        if (pendantsPackages == 0) {
            if(incidences > 0) {
                parcelStatusDeliver = PARCEL_STATUS.DELIVERED_WITH_INCIDENCES;
            } else {
                parcelStatusDeliver = PARCEL_STATUS.DELIVERED_OK;
            }
        } else {
            parcelStatusDeliver = PARCEL_STATUS.DELIVERED_PARTIAL;
        }
        return parcelStatusDeliver;
    }

    private Future<Integer> checkPendants(SQLConnection conn, Integer parcelId){
        Future<Integer> future = Future.future();
        conn.queryWithParams(QUERY_CHECK_PENDANT_PACKAGES, new JsonArray().add(parcelId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                future.complete(reply.result().getNumRows());
            } catch (Throwable t) {
                future.fail(t);
            }
        });
        return future;
    }

    private Future<Integer> checkIncidences(SQLConnection conn, Integer parcelId){
        Future<Integer> future = Future.future();
        conn.queryWithParams(QUERY_CHECK_INCIDENTED_PACKAGES, new JsonArray().add(parcelId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                future.complete(reply.result().getNumRows());
            } catch (Throwable t) {
                t.printStackTrace();
                future.fail(t);
            }
        });
        return future;
    }

    private CompletableFuture<Integer> createParcelDelivery(SQLConnection conn, JsonObject deliverBody){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            Integer parcelId = deliverBody.getInteger("id");
            Integer updatedBy = deliverBody.getInteger("updated_by");
            final String credentialType = deliverBody.getString("credential_type");
            final String noCredential = deliverBody.getString("no_credential");

            JsonObject parcelDelivery = new JsonObject()
                    .put("credential_type", credentialType)
                    .put("no_credential", noCredential)
                    .put("created_by", updatedBy);

            if (deliverBody.getString("signature") != null){
                parcelDelivery
                        .put("signature", deliverBody.getString("signature"));
            }

            if (deliverBody.getString("addressee_name") != null && deliverBody.getString("addressee_last_name") != null){
                parcelDelivery
                        .put("name", deliverBody.getString("addressee_name"))
                        .put("last_name", deliverBody.getString("addressee_last_name"));
            }

            if (parcelDelivery.getString("name") != null && parcelDelivery.getString("last_name") != null){
                this.registerParcelDelivery(conn, parcelDelivery).whenComplete((parcelDeliveryId, errorParcelDelivery) -> {
                    try{
                        if (errorParcelDelivery != null){
                            throw errorParcelDelivery;
                        }
                        future.complete(parcelDeliveryId);
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            } else {
                conn.queryWithParams(QUERY_GET_ADDRESSEE_INFO_BY_PARCEL_ID, new JsonArray().add(parcelId), replyAddresseeInfo -> {
                    try{
                        if (replyAddresseeInfo.failed()) {
                            throw replyAddresseeInfo.cause();
                        }
                        JsonObject resultAddresseeInfo = replyAddresseeInfo.result().getRows().get(0);
                        parcelDelivery
                                .put("name", resultAddresseeInfo.getString("addressee_name"))
                                .put("last_name", resultAddresseeInfo.getString("addressee_last_name"));
                        this.registerParcelDelivery(conn, parcelDelivery).whenComplete((parcelDeliveryId, errorParcelDelivery) -> {
                            try{
                                if (errorParcelDelivery != null){
                                    throw errorParcelDelivery;
                                }
                                future.complete(parcelDeliveryId);
                            } catch (Throwable t){
                                future.completeExceptionally(t);
                            }
                        });
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            }
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> deliverPackage(SQLConnection conn, JsonObject parcelPackage, int updateBy, Integer parcelId, int parcelsAddresseeId, Integer empBranchofficeId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        Integer parcelPackageId= parcelPackage.getInteger("id");
        Integer packageStatusCode = parcelPackage.getInteger("package_status");

        this.checkParcelPackageBelongsParcel(conn, parcelId, parcelPackageId).whenComplete((resultCheckParcelPackageBelongsParcel, errorCheckParcelPackageBelongsParcel) ->{
            try{
                if (errorCheckParcelPackageBelongsParcel != null){
                    future.completeExceptionally(errorCheckParcelPackageBelongsParcel);
                } else {
                    this.checkEmployeeBranchParcelPackage(conn, parcelId, empBranchofficeId).whenComplete((resultCheckEmployeeBranchParcelPackage, errorCheckEmployeeBranchParcelPackage) -> {
                        try{
                            if (errorCheckEmployeeBranchParcelPackage != null){
                                future.completeExceptionally(errorCheckEmployeeBranchParcelPackage);
                            } else {
                                this.checkParcelDestiny(conn, parcelId, resultCheckEmployeeBranchParcelPackage).whenComplete((resultCheckParcelDestiny, errorCheckParcelDestiny) -> {
                                    try{
                                        if(errorCheckParcelDestiny != null){
                                            future.completeExceptionally(errorCheckParcelDestiny);
                                        } else {
                                            String updatePackage = "UPDATE parcels_packages\n" +
                                                    "SET package_status= ? , updated_by = ? , parcels_deliveries_id = ? \n"
                                                    + "WHERE id = ?;";

                                            JsonArray params = new JsonArray()
                                                    .add(packageStatusCode)
                                                    .add(updateBy)
                                                    .add(parcelsAddresseeId)
                                                    .add(parcelPackageId);

                                            conn.updateWithParams(updatePackage, params, replyUpdate -> {
                                                try {
                                                    if (replyUpdate.succeeded()) {
                                                        if (packageStatusCode.equals(PARCEL_STATUS.DELIVERED_WITH_INCIDENCES.ordinal())) {
                                                            parcelPackage.put("updateBy", updateBy);
                                                            this.insertParcelIncidence(conn, parcelPackage, parcelId).whenComplete((result, stThrow) -> {
                                                                try{
                                                                    if (stThrow != null) {
                                                                        future.completeExceptionally(stThrow);
                                                                    } else {
                                                                        result.put("parcel_package_id", parcelPackage.getInteger("id"));
                                                                        future.complete(result);
                                                                    }
                                                                } catch (Exception e){
                                                                    future.completeExceptionally(e);
                                                                }
                                                            });
                                                        } else {
                                                            future.complete(parcelPackage);
                                                        }
                                                    } else {
                                                        future.completeExceptionally(replyUpdate.cause());
                                                    }
                                                } catch (Exception e){
                                                    future.completeExceptionally(e);
                                                }
                                            });
                                        }
                                    } catch (Exception e){
                                        future.completeExceptionally(e);
                                    }
                                });
                            }
                        } catch (Exception e){
                            future.completeExceptionally(e);
                        }
                    });
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private CompletableFuture<Boolean> checkParcelPackageBelongsParcel(SQLConnection conn, Integer parcelId, Integer parcelPackageId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            JsonArray param = new JsonArray().add(parcelPackageId);
            conn.queryWithParams("SELECT parcel_id FROM parcels_packages WHERE id = ?;", param, reply ->{
                try{
                    if(reply.succeeded()){
                        Integer referenceParcelId = reply.result().getRows().get(0).getInteger("parcel_id");
                        if(referenceParcelId.equals(parcelId)){
                            future.complete(true);
                        } else {
                            future.completeExceptionally(new Throwable("The parcel_package_id:"+parcelPackageId+" does not match the parcel_id"));
                        }
                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch(Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }
        return future;
    }

    private CompletableFuture<Integer> checkEmployeeBranchParcelPackage(SQLConnection conn, Integer parcelId, Integer empBranchofficeId){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            JsonArray param = new JsonArray().add(empBranchofficeId).add(parcelId);
            conn.queryWithParams("SELECT \n" +
                    "   ppt.id, \n" +
                    "   ppt.terminal_id, \n" +
                    "   ppt.action,\n" +
                    "   p.terminal_destiny_id,\n" +
                    "   (ppt.terminal_id IN " +
                    "       (SELECT bprc.receiving_branchoffice_id FROM branchoffice_parcel_receiving_config bprc \n" +
                    "       WHERE bprc.of_branchoffice_id = p.terminal_destiny_id)\n" +
                    "       OR\n" +
                    "        ? IN\n" +
                    "       (SELECT bprc.receiving_branchoffice_id FROM branchoffice_parcel_receiving_config bprc\n" +
                    "       WHERE bprc.of_branchoffice_id = p.terminal_destiny_id)\n" +
                    ") AS in_replacement_terminal\n" +
                    "FROM parcels_packages_tracking ppt\n" +
                    "INNER JOIN parcels p ON p.id = ppt.parcel_id\n" +
                    "WHERE ppt.parcel_id = ? \n" +
                    "AND ppt.action IN('move', 'downloaded', 'arrived', 'located') \n" +
                    "ORDER BY ppt.created_at DESC \n" +
                    "LIMIT 1;", param, reply ->{
                try{
                    if(reply.succeeded()){
                        JsonObject result = reply.result().getRows().get(0);
                        Integer referenceTerminalId = result.getInteger("terminal_id");
                        String referenceAction = result.getString("action");
                        Integer terminalDestinyId = result.getInteger(_TERMINAL_DESTINY_ID);
                        boolean inReplacementTerminal = result.getInteger("in_replacement_terminal") > 0;
                        if ((referenceTerminalId.intValue() != empBranchofficeId.intValue() && !inReplacementTerminal) || ((referenceTerminalId.intValue() != terminalDestinyId) && !inReplacementTerminal)){
                            future.completeExceptionally(new Throwable("Packages can not be delivered to this branch"));
                        } else {
                            if(referenceAction.equals("move") || referenceAction.equals("downloaded") || referenceAction.equals("arrived")  || referenceAction.equals("located")){
                                future.complete(terminalDestinyId);
                            } else {
                                future.completeExceptionally(new Throwable("The package has not been arrived, downloaded or moved between departments"));
                            }
                        }
                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch(Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<Boolean> checkParcelDestiny(SQLConnection conn, Integer parcelId, Integer referenceTerminalId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            JsonArray param = new JsonArray().add(parcelId);
            conn.queryWithParams("SELECT id, terminal_destiny_id FROM parcels WHERE id = ?;", param, reply ->{
                try{
                    if(reply.succeeded()){
                        Integer terminalDestinyId = reply.result().getRows().get(0).getInteger("terminal_destiny_id");
                        if(terminalDestinyId.equals(referenceTerminalId)){
                            future.complete(true);
                        } else {
                            future.completeExceptionally(new Throwable("The package is not in the destination terminal"));
                        }
                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch(Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<JsonObject> insertParcelIncidence(SQLConnection conn,JsonObject parcelPackage, int parcelId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray incidences = parcelPackage.getJsonArray("packages_incidences");

            JsonObject parcelIncidence = new JsonObject();
            JsonObject parcelPackageTracking = new JsonObject();

            List<String> parcelPackageTrackings = new ArrayList<>();
            List<String> parcelIncidences = new ArrayList<>();

            for (int i = 0; i < incidences.size(); i++) {
                JsonObject incidence = incidences.getJsonObject(i);

                parcelIncidence.put("parcel_id", parcelId);
                parcelIncidence.put("parcel_package_id",parcelPackage.getInteger("id"));
                parcelIncidence.put("incidence_id",incidence.getInteger("incidence_id"));
                parcelIncidence.put("notes",incidence.getString("notes"));
                parcelIncidence.put("status",parcelPackage.getInteger("package_status"));
                parcelIncidence.put("created_by",parcelPackage.getInteger("updateBy"));
                parcelIncidences.add(dbVerticle.generateGenericCreate("parcels_incidences", parcelIncidence));


                parcelPackageTracking.put("parcel_id", parcelId);
                parcelPackageTracking.put("parcel_package_id",parcelPackage.getInteger("id"));
                parcelPackageTracking.put("action","closed");
                parcelPackageTracking.put("notes",incidence.getString("notes"));
                parcelPackageTracking.put("status",parcelPackage.getInteger("package_status"));
                parcelPackageTracking.put("created_by",parcelPackage.getInteger("updateBy"));
                parcelPackageTrackings.add(dbVerticle.generateGenericCreate("parcels_packages_tracking", parcelPackageTracking));

            }

            conn.batch(parcelIncidences, (AsyncResult<List<Integer>> replyInsert) -> {
                try {
                    if (replyInsert.succeeded()) {

                        this.insertParcelPackageTrackingDeliver(conn, parcelPackageTrackings, parcelPackage).whenComplete((result, stThrow) -> {
                            try {
                                if (stThrow != null) {
                                    future.completeExceptionally(stThrow);
                                } else {
                                    result.put("parcel_package_id", parcelPackage.getInteger("id"));
                                    future.complete(result);
                                }
                            } catch (Exception ex) {
                                future.completeExceptionally(ex);
                            }
                        });

                    } else {
                        future.completeExceptionally(replyInsert.cause());
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<JsonObject> insertParcelPackageTrackingDeliver(SQLConnection conn, List<String> parcelPackageTrackings,JsonObject parcelPackage) {

        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            conn.batch(parcelPackageTrackings, (AsyncResult<List<Integer>> replyInsert ) -> {
                try {
                    if (replyInsert.succeeded()) {
                        future.complete(parcelPackage);
                    } else {
                        future.completeExceptionally(replyInsert.cause());
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });

        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }

    public CompletableFuture<Boolean> insertTracking(SQLConnection conn, JsonArray items, String trackingTable, String principalField, String reference, String notes, String action, Integer createdBy){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            List<String> batch = new ArrayList<>();
            for(int i=0; i<items.size(); i++){
                JsonObject obj = items.getJsonObject(i);
                JsonObject track = new JsonObject()
                        .put(reference, obj.getInteger("id"))
                        .put("action", action)
                        .put("created_by", createdBy);
                if(principalField != null){
                    track.put(principalField, obj.getInteger(principalField));
                }
                if(obj.getInteger("terminal_id") != null){
                    track.put("terminal_id", obj.getInteger("terminal_id"));
                }
                if(action.equals("printed")){
                    Integer prints = obj.getInteger("prints_counter");
                    if(prints == 1){
                        track.put("notes", "Impresión");
                    } else {
                        int reprints = prints-1;
                        track.put("notes", "Reimpresión #"+reprints);
                    }
                } else {
                    track.put("notes", notes);
                }
                batch.add(dbVerticle.generateGenericCreate(trackingTable, track));
            }
            conn.batch(batch, batchReply -> {
                try {
                    if (batchReply.failed()){
                        throw batchReply.cause();
                    }
                    future.complete(true);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> insertTicket(SQLConnection conn, Integer cashOutId, Integer parcelId, Double totalPayments, JsonObject cashChange, Integer createdBy, Double ivaPercent, String action) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject ticket = new JsonObject();
            Double iva = this.getIva(totalPayments, ivaPercent);

            // Create ticket_code
            // ticket.put("ticket_code", ticketCode);
            ticket.put("parcel_id", parcelId);
            ticket.put("cash_out_id", cashOutId);
            ticket.put("iva", iva);
            ticket.put("total", totalPayments);
            ticket.put("created_by", createdBy);
            ticket.put("ticket_code", UtilsID.generateID("T"));
            if(action != null){
                ticket.put("action", action);
            }

            if(cashChange != null){
                Double paid = cashChange.getDouble("paid");
                Double total = cashChange.getDouble("total");
                Double paid_change = UtilsMoney.round(cashChange.getDouble("paid_change"), 2);
                Double difference_paid = UtilsMoney.round(paid - total, 2);

                ticket.put("paid", paid);
                ticket.put("paid_change", paid_change);

                if(!Objects.equals(action, "voucher")) {
                    if (totalPayments < total) {
                        throw new Throwable("The payment " + total + " is greater than the total " + totalPayments);
                    } else if (totalPayments > total) {
                        throw new Throwable("The payment " + total + " is lower than the total " + totalPayments);
                    } else if (paid_change > difference_paid) {
                        throw new Throwable("The change " + paid_change + " is greater than the difference between paid and payments (" + paid + " - " + total + ")");
                    } else if (paid_change < difference_paid) {
                        throw new Throwable("The change " + paid_change + " is lower than the difference between paid and payments (" + paid + " - " + total + ")");
                    }
                }
            } else {
                ticket.put("paid", totalPayments);
                ticket.put("paid_change", 0.0);
            }

            GenericQuery insert = this.generateGenericCreate("tickets", ticket);
            conn.updateWithParams(insert.getQuery(), insert.getParams(), (AsyncResult<UpdateResult> reply) -> {
                try{
                    if (reply.succeeded()) {
                        final int id = reply.result().getKeys().getInteger(0);
                        ticket.put("id", id);
                        future.complete(ticket);
                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Throwable ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<Boolean> insertTicketDetail(SQLConnection conn, Integer ticketId, Integer createdBy, JsonArray packages, JsonArray packings, JsonObject parcel, JsonObject serviceObject) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        List<String> inserts = new ArrayList<>();

        conn.query("SELECT id, name_price, shipping_type FROM package_price;", replyPP -> {
            try{
                if(replyPP.failed()){
                    throw new Exception(replyPP.cause());
                }
                List<JsonObject> resultPP = replyPP.result().getRows();
                Map<String, List<JsonObject>> groupedPackages = packages.stream().map(x -> (JsonObject)x).collect(Collectors.groupingBy(w -> w.getString("shipping_type")));
                JsonArray details = new JsonArray();

                for (String s : groupedPackages.keySet()) {
                    JsonObject packagePrice = new JsonObject();
                    AtomicReference<Integer> quantity = new AtomicReference<>(0);
                    AtomicReference<Double> unitPrice = new AtomicReference<>(0.00);
                    AtomicReference<Double> amount = new AtomicReference<>(0.00);
                    Optional<JsonObject> packageName = resultPP.stream().filter(x -> x.getInteger("id").equals(groupedPackages.get(s).get(0).getInteger("package_price_id"))).findFirst();
                    String packageRange = packageName.get().getString("name_price");
                    groupedPackages.get(s).forEach(x -> {
                        quantity.getAndSet(quantity.get() + 1);
                        unitPrice.updateAndGet(v -> v + x.getDouble("total_amount"));
                        amount.updateAndGet(v -> v + x.getDouble("total_amount"));
                        packagePrice.put(_DISCOUNT, x.getDouble(_DISCOUNT));
                    });
                    packagePrice.put("shipping_type", s);
                    packagePrice.put("unit_price", unitPrice.get());
                    packagePrice.put("amount", amount.get());
                    packagePrice.put("quantity", quantity.get());
                    if(packagePrice.getInteger("quantity") != null){
                        if(packagePrice.getInteger("quantity") > 0){
                            JsonObject ticketDetail = new JsonObject();
                            String shippingType = packagePrice.getString("shipping_type");
                            switch (shippingType){
                                case "parcel":
                                    shippingType = "paquetería";
                                    break;
                                case "courier":
                                    shippingType = "mensajería";
                                    break;
                                case "pets":
                                    shippingType = "mascota";
                                    break;
                                case "frozen":
                                    shippingType = "carga refrigerada";
                                    break;
                            }
                            ticketDetail.put("ticket_id", ticketId);
                            ticketDetail.put("quantity", packagePrice.getInteger("quantity"));
                            ticketDetail.put("detail", "Envío de " + shippingType + " con rango " + packageRange);
                            ticketDetail.put("unit_price", packagePrice.getDouble("unit_price"));
                            ticketDetail.put(_DISCOUNT, packagePrice.getDouble(_DISCOUNT));
                            ticketDetail.put("amount", packagePrice.getDouble("amount"));
                            ticketDetail.put("created_by", createdBy);
                            details.add(ticketDetail);
                        }
                    }
                }

                int len = packings.size();
                for (int i = 0; i < len; i++) {
                    JsonObject packing = packings.getJsonObject(i);
                    JsonObject ticketDetail = new JsonObject();

                    ticketDetail.put("ticket_id", ticketId);
                    ticketDetail.put("quantity", packing.getInteger("quantity"));
                    ticketDetail.put("detail", "Embalaje");
                    ticketDetail.put("unit_price", packing.getDouble("unit_price"));
                    ticketDetail.put("amount", packing.getDouble("amount"));
                    ticketDetail.put("created_by", createdBy);

                    details.add(ticketDetail);
                }

                if(parcel.getBoolean("has_insurance") != null && parcel.getBoolean("has_insurance")){
                    JsonObject ticketDetail = new JsonObject();
                    ticketDetail.put("ticket_id", ticketId);
                    ticketDetail.put("quantity", 1);
                    ticketDetail.put("detail", "Seguro de envío");
                    ticketDetail.put("unit_price", parcel.getDouble("insurance_amount"));
                    ticketDetail.put("amount", parcel.getDouble("insurance_amount"));
                    ticketDetail.put("created_by", createdBy);

                    details.add(ticketDetail);
                }

                if(serviceObject.getBoolean("is_rad") != null || serviceObject.getBoolean("is_ead") != null || serviceObject.getBoolean("is_rad_ead") != null){
                    JsonObject ticketDetail = new JsonObject();
                    ticketDetail.put("ticket_id", ticketId);
                    ticketDetail.put("quantity", 1);
                    ticketDetail.put("detail", "Servicio " +
                            serviceObject.getString("service"));
                    ticketDetail.put("unit_price", serviceObject.getDouble("service_amount"));
                    ticketDetail.put("amount", serviceObject.getDouble("service_amount"));
                    ticketDetail.put("created_by", createdBy);

                    details.add(ticketDetail);
                }


                if(packages.isEmpty() && packings.isEmpty()) {
                    JsonObject ticketDetail = new JsonObject()
                            .put("ticket_id", ticketId)
                            .put("quantity", 0.00)
                            .put("detail", "Comprobante de entrega de paquetería")
                            .put("unit_price", 0.00)
                            .put("amount", 0.00)
                            .put("created_by", createdBy);

                    details.add(ticketDetail);
                }

                for(int i = 0; i < details.size(); i++){
                    inserts.add(dbVerticle.generateGenericCreate("tickets_details", details.getJsonObject(i)));
                }

                conn.batch(inserts, (AsyncResult<List<Integer>> replyInsert) -> {
                    try {
                        if (replyInsert.failed()){
                            throw new Exception(replyInsert.cause());
                        }
                        future.complete(replyInsert.succeeded());
                    }catch(Exception e ){
                        future.completeExceptionally(e);
                    }
                });


            }catch (Exception ex){
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> insertPaymentAndCashOutMove(SQLConnection conn, JsonObject payment, Integer currencyId, Integer packageId, Integer cashOutId, Integer createdBy, Boolean is_credit) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            if (!is_credit) {
                JsonObject cashOutMove = new JsonObject();
                payment.put("currency_id", currencyId);
                payment.put("parcel_id", packageId);
                payment.put("created_by", createdBy);
                cashOutMove.put("quantity", payment.getDouble("amount"));
                cashOutMove.put("move_type", "0");
                cashOutMove.put("cash_out_id", cashOutId);
                cashOutMove.put("created_by", createdBy);
                //String insert = this.generateGenericCreate("payment", payment);
                PaymentDBV objPayment = new PaymentDBV();
                objPayment.insertPayment(conn, payment).whenComplete((resultPayment, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        payment.put("id", resultPayment.getInteger("id"));
                        cashOutMove.put("payment_id", resultPayment.getInteger("id"));
                        GenericQuery insertCashOutMove = this.generateGenericCreate("cash_out_move", cashOutMove);
                        conn.updateWithParams(insertCashOutMove.getQuery(), insertCashOutMove.getParams(), (AsyncResult<UpdateResult> replyMove) -> {
                            try {
                                if (replyMove.failed()) {
                                    throw new Exception(replyMove.cause());
                                }
                                future.complete(payment);

                            } catch (Exception e) {
                                future.completeExceptionally(e);
                            }
                        });
                    }
                });
            } else {
                future.complete(payment);
            }
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<JsonObject> updateParcelStatus(SQLConnection conn, Integer parcelId, Integer parcelStatus, Double payback, boolean isCredit, int updatedBy) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject bodyUpdate = new JsonObject().put(ID, parcelId)
                    .put("parcel_status", parcelStatus)
                    .put("delivered_at", sdfDataBase(new Date()))
                    .put(UPDATED_BY, updatedBy);

            if (payback != null){
                bodyUpdate.put("payback", payback);
            }

            if (isCredit){
                bodyUpdate.put("payment_condition", "credit");
            }

            GenericQuery updateParcel = this.generateGenericUpdate("parcels", bodyUpdate);
            conn.updateWithParams(updateParcel.getQuery(), updateParcel.getParams(), replyUpdate -> {
                try {
                    if (replyUpdate.failed()) {
                        throw new Exception(replyUpdate.cause());
                    }

                    this.updateParcelManifestDetail(conn, parcelId, updatedBy).whenComplete((res, err) -> {
                        try {
                            if (err != null) {
                                throw err;
                            }

                            future.complete(replyUpdate.result().toJson());
                        } catch (Throwable t) {
                            future.completeExceptionally(t);
                        }
                    });
                } catch (Exception ex) {
                    future.completeExceptionally(ex);
                }
            });
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<Integer> registerParcelDelivery(SQLConnection conn, JsonObject paramInsertParcelsDeliveries){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            GenericQuery insert = this.generateGenericCreate("parcels_deliveries", paramInsertParcelsDeliveries);
            conn.updateWithParams(insert.getQuery(), insert.getParams(), replyInsertParcelsDeliveries -> {
                try{
                    if (replyInsertParcelsDeliveries.failed()){
                        throw replyInsertParcelsDeliveries.cause();
                    }
                    Integer parcelsDeliveriesId = replyInsertParcelsDeliveries.result().getKeys().getInteger(0);
                    future.complete(parcelsDeliveriesId);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<Boolean> updateParcelManifestDetail(SQLConnection conn, int parcelId, int updatedBy) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            getParcelManifestDetail(conn, parcelId).whenComplete((parcelManifestDetailId, errPMD) -> {
                try {
                    if (errPMD != null) {
                        throw errPMD;
                    }

                    if (Objects.isNull(parcelManifestDetailId)) {
                        future.complete(true);
                        return;
                    }

                    GenericQuery update = this.generateGenericUpdate("parcels_manifest_detail",
                            new JsonObject()
                                    .put(STATUS, PARCEL_MANIFEST_DETAIL_STATUS.DELIVERED.ordinal())
                                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()))
                                    .put(UPDATED_BY, updatedBy)
                                    .put(ID, parcelManifestDetailId));
                    conn.updateWithParams(update.getQuery(), update.getParams(), reply -> {
                        try {
                            if (reply.failed()) {
                                throw reply.cause();
                            }

                            future.complete(true);
                        } catch (Throwable t) {
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

    private CompletableFuture<Integer> getParcelManifestDetail(SQLConnection conn, int parcelId) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            JsonArray param  = new JsonArray().add(parcelId);
            conn.queryWithParams(GET_PARCEL_MANIFEST_DETAIL_BY_PARCEL_ID, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        future.complete(null);
                    } else {
                        future.complete(result.get(0).getInteger(ID));
                    }
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private Double getIva(Double amount, Double ivaPercent){
        return amount - (amount / (1 + (ivaPercent/100)));
    }

    private static final String QUERY_CHECK_PENDANT_PACKAGES = "SELECT * FROM parcels_packages \n" +
            "WHERE parcel_id = ?\n" +
            "AND package_status IN (" + PARCEL_STATUS.DOCUMENTED.ordinal() + ", "+ PARCEL_STATUS.IN_TRANSIT.ordinal() +", "+ PARCEL_STATUS.ARRIVED.ordinal() + ");";

    private static final String QUERY_PARCEL_PACKAGES = "SELECT \n" +
            "   pp.*, \n" +
            "   pp.id AS parcel_package_id, \n" +
            "   p.waybill,\n" +
            "   pprice.name_price AS package_price_name\n" +
            "FROM parcels_packages pp\n" +
            "INNER JOIN package_price pprice ON pprice.id = pp.package_price_id\n" +
            "JOIN parcels AS p ON p.id=pp.parcel_id\n" +
            "WHERE pp.parcel_id = ? ";

    private static final String QUERY_CHECK_INCIDENTED_PACKAGES = QUERY_PARCEL_PACKAGES +
            "AND pp.package_status = " + PARCEL_STATUS.DELIVERED_WITH_INCIDENCES.ordinal() + ";";

    private static final String QUERY_GET_ADDRESSEE_INFO_BY_PARCEL_ID = "SELECT addressee_name, addressee_last_name FROM parcels WHERE id = ?;";

    private static final String GET_PARCEL_MANIFEST_DETAIL_BY_PARCEL_ID = "SELECT pmd.id FROM parcels_manifest_detail pmd\n" +
            "INNER JOIN parcels_manifest pm ON pm.id = pmd.id_parcels_manifest\n" +
            "INNER JOIN parcels_rad_ead pre ON pre.id = pmd.id_parcels_rad_ead\n" +
            "INNER JOIN parcels p ON p.id = pre.parcel_id\n" +
            "WHERE p.id = ?\n" +
            "   AND pmd.status = "+ PARCEL_MANIFEST_DETAIL_STATUS.OPEN.ordinal() +" \n" +
            "   AND pm.status IN ("+ PARCEL_MANIFEST_STATUS.OPEN.ordinal() +", "+ PARCEL_MANIFEST_STATUS.CLOSE.ordinal() +") \n" +
            "GROUP BY pmd.id;";
}
