package database.promos.handlers.PromosDBV;

import database.commons.DBHandler;
import database.parcel.enums.SHIPMENT_TYPE;
import database.promos.PromosDBV;
import database.promos.enums.SERVICES;
import database.promos.handlers.PromosDBV.models.ParcelPackage;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.promos.PromosDBV.ACTION_CALCULATE_MULTIPLE_PROMO;
import static database.promos.PromosDBV.SERVICE;
import static service.commons.Constants.*;
import static utils.UtilsValidation.*;

public class ApplyMultiplePromo extends DBHandler<PromosDBV> {

    public ApplyMultiplePromo(PromosDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        try {
            isEmptyAndNotNull(body, _ORIGIN);
            isContainedAndNotNull(body, SERVICE, SERVICES.parcel.name(), SERVICES.parcel_inhouse.name());
            isGraterAndNotNull(body, _TERMINAL_ORIGIN_ID, 0);
            isGraterAndNotNull(body, _TERMINAL_DESTINY_ID, 0);
            isContainedAndNotNull(_SHIPMENT_TYPE, body, _SHIPMENT_TYPE, SHIPMENT_TYPE.OCU.getValue(), SHIPMENT_TYPE.RAD_EAD.getValue(), SHIPMENT_TYPE.EAD.getValue(), SHIPMENT_TYPE.RAD_OCU.getValue());
            isGrater(body, _CUSTOMER_BILLING_INFORMATION_ID, 0);
            isBooleanAndNotNull(body, _PAYS_SENDER);
            isGraterAndNotNull(body, _SENDER_ID, 0);
            if(!body.getBoolean(_PAYS_SENDER)) {
                isGraterAndNotNull(body, _ADDRESSEE_ID, 0);
            } else {
                isGrater(body, _ADDRESSEE_ID, 0);
            }
            isEmptyAndNotNull(body.getJsonArray(_PARCEL_PACKAGES), _PARCEL_PACKAGES);
            JsonArray parcelPackages = body.getJsonArray(_PARCEL_PACKAGES);
            for (Object p : parcelPackages) {
                JsonObject parcelPackage = (JsonObject) p;
                isContainedAndNotNull(parcelPackage, _SHIPPING_TYPE, "parcel", "courier");
                isEmpty(parcelPackage, _CONTAINS);
                isGrater(parcelPackage, _PACKAGE_TYPE_ID, 0);
                isGraterAndNotNull(parcelPackage, _QUANTITY, 0);
                if (parcelPackage.getString(_SHIPPING_TYPE).equals("parcel")) {
                    isGraterAndNotNull(parcelPackage, _WEIGHT, 0.0);
                    isGraterAndNotNull(parcelPackage, _HEIGHT, 0.0);
                    isGraterAndNotNull(parcelPackage, _WIDTH, 0.0);
                    isGraterAndNotNull(parcelPackage, _LENGTH, 0.0);
                }
            }

            this.getVertx().eventBus().send(PromosDBV.class.getSimpleName(), body,
                    new DeliveryOptions().addHeader(ACTION, ACTION_CALCULATE_MULTIPLE_PROMO),
                    (AsyncResult<Message<JsonObject>> reply) -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    JsonObject responseCalculate = reply.result().body();
                    List<ParcelPackage> parcelPackagesPromo = responseCalculate.getJsonArray(_PARCEL_PACKAGES)
                            .stream().map(p -> {
                                JsonObject pack = (JsonObject) p;
                                return pack.mapTo(ParcelPackage.class);
                            }).collect(Collectors.toList());

                    List<CompletableFuture<Boolean>> taskUpdatePromoUsage = new ArrayList<>();
                    startTransaction(message, conn -> {
                        try {
                            for (ParcelPackage parcelPackage : parcelPackagesPromo) {
                                Integer promoId = parcelPackage.getPromoId();
                                if (Objects.nonNull(promoId)) {
                                    taskUpdatePromoUsage.add(updatePromoUsage(conn, promoId));
                                }
                                Integer customerPromoId = parcelPackage.getCustomersPromosId();
                                if (Objects.nonNull(customerPromoId)) {
                                    taskUpdatePromoUsage.add(updateCustomerPromoUsage(conn, customerPromoId));
                                }
                            }
                            CompletableFuture.allOf(taskUpdatePromoUsage.toArray(new CompletableFuture[taskUpdatePromoUsage.size()])).whenComplete((replyUpdates, errUpdates) -> {
                                try {
                                    if (errUpdates != null) {
                                        throw errUpdates;
                                    }
                                    this.commit(conn, message, responseCalculate);
                                } catch (Throwable t) {
                                    this.rollback(conn, t, message);
                                }
                            });
                        } catch (Throwable t) {
                            this.rollback(conn, t, message);
                        }
                    });

                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (PropertyValueException ex){
            reportQueryError(message, ex);
        }
    }

    private CompletableFuture<Boolean> updatePromoUsage(SQLConnection conn, int promoId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        conn.updateWithParams(QUERY_UPDATE_PROMO_USAGE_BY_ID, new JsonArray().add(promoId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                future.complete(true);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<Boolean> updateCustomerPromoUsage(SQLConnection conn, int customerPromoId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        conn.updateWithParams(QUERY_UPDATE_CUSTOMER_PROMO_USAGE_BY_ID, new JsonArray().add(customerPromoId), reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                future.complete(true);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private static final String QUERY_UPDATE_PROMO_USAGE_BY_ID = "UPDATE promos SET used = used + 1 WHERE id = ?;";

    private static final String QUERY_UPDATE_CUSTOMER_PROMO_USAGE_BY_ID = "UPDATE customers_promos SET used = used + 1 WHERE id = ?;";

}
