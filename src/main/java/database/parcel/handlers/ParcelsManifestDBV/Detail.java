package database.parcel.handlers.ParcelsManifestDBV;

import database.commons.DBHandler;
import database.money.CashOutDBV;
import database.parcel.ParcelsManifestDBV;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCEL_MANIFEST_DETAIL_STATUS;
import database.parcel.enums.PARCEL_MANIFEST_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.*;

public class Detail extends DBHandler<ParcelsManifestDBV> {

    public Detail(ParcelsManifestDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer id = body.getInteger(ID);
            JsonArray paymentMethods = body.getJsonArray("payment_methods");

            getActiveParcelsManifest(id).whenComplete((parcelManifest, errVRE) -> {
                try {
                    if (errVRE != null) {
                        throw errVRE;
                    }

                    List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                    tasks.add(getParcelsInfoByManifest(parcelManifest));
                    tasks.add(getCashoutExtendedInfo(parcelManifest, paymentMethods));

                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((res, err) -> {
                        try {
                            if (err != null) {
                                throw err;
                            }

                            message.reply(parcelManifest);
                        } catch (Throwable t) {
                            reportQueryError(message, t);
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

    private CompletableFuture<JsonObject> getActiveParcelsManifest(int id) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray param = new JsonArray().add(id);
            this.dbClient.queryWithParams(QUERY_GET_ACTIVE_PARCELS_MANIFEST, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if (results.isEmpty()) {
                        throw new Exception("Parcel manifest not found");
                    }
                    future.complete(results.get(0));
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> getParcelsInfoByManifest(JsonObject parcelManifest) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Integer parcelManifestId = parcelManifest.getInteger(ID);
            this.dbClient.queryWithParams(QUERY_GET_PARCELS_BY_MANIFEST, new JsonArray().add(parcelManifestId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> parcels = reply.result().getRows();
                    parcelManifest.put(_PARCELS, parcels);
                    if (parcels.isEmpty()) {
                        future.complete(true);
                        return;
                    }

                    List<CompletableFuture<Boolean>> tasks = new ArrayList<>();
                    for (JsonObject parcel : parcels) {
                        tasks.add(getPackagesInfoByParcel(parcelManifestId, parcel));
                        tasks.add(getAddressesByParcel(parcel));
                    }

                    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((res, err) -> {
                        try {
                            if (err != null) {
                                throw err;
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

    private CompletableFuture<Boolean> getPackagesInfoByParcel(int parcelManifestId, JsonObject parcel) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Integer parcelId = parcel.getInteger(ID);
            JsonArray params = new JsonArray()
                    .add(parcelManifestId).add(parcelId);
            this.dbClient.queryWithParams(QUERY_GET_PACKAGES_BY_PARCEL, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> packages = reply.result().getRows();
                    parcel.put(_PARCEL_PACKAGES, packages);

                    future.complete(true);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> getAddressesByParcel(JsonObject parcel) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Integer parcelId = parcel.getInteger(ID);
            JsonArray params = new JsonArray().add(parcelId).add(parcelId);
            this.dbClient.queryWithParams(QUERY_GET_ADDRESS_INFO_BY_PARCEL, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> addresses = reply.result().getRows();
                    if(!addresses.isEmpty()) {
                        boolean isSender = addresses.get(0).getInteger("is_sender") == 1;
                        if (isSender) {
                            parcel.put("sender_address", addresses.get(0));
                            parcel.put("addressee_address", addresses.get(1));
                        } else {
                            parcel.put("sender_address", addresses.get(1));
                            parcel.put("addressee_address", addresses.get(0));
                        }
                    }

                    future.complete(true);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> getCashoutExtendedInfo(JsonObject parcelManifest, JsonArray paymentMethods) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            Integer cashOutId = parcelManifest.getInteger(CASHOUT_ID);
            if (Objects.isNull(cashOutId)) {
                future.complete(true);
                return future;
            }
            JsonObject params = new JsonObject()
                    .put(CASHOUT_ID, cashOutId)
                    .put("payment_methods", paymentMethods);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CashOutDBV.ACTION_CASH_OUT_EXTENDED);
            this.getVertx().eventBus().send(CashOutDBV.class.getSimpleName(), params, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    JsonObject result = (JsonObject) reply.result().body();
                    parcelManifest.put("cash_out_info", result);
                    future.complete(true);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private static final String QUERY_GET_ACTIVE_PARCELS_MANIFEST = "SELECT\n" +
            "   pm.*,\n" +
            "   co.cash_register_id\n" +
            "FROM parcels_manifest pm \n" +
            "INNER JOIN users u ON u.id = pm.created_by\n" +
            "INNER JOIN employee e ON e.user_id = u.id\n" +
            "LEFT JOIN cash_out co ON co.id = pm.cash_out_id\n" +
            "WHERE pm.id = ?\n" +
            "   AND pm.status IN ("
                + PARCEL_MANIFEST_STATUS.OPEN.ordinal() +", \n" +
                + PARCEL_MANIFEST_STATUS.CLOSE.ordinal() +");";

    private static final String QUERY_GET_PARCELS_BY_MANIFEST = "SELECT\n" +
            "   pmd.id AS parcels_manifest_detail_id,\n" +
            "   pmd.status AS parcels_manifest_detail_status,\n" +
            "   p.*\n" +
            "FROM parcels_manifest pm\n" +
            "INNER JOIN parcels_manifest_detail pmd ON pmd.id_parcels_manifest = pm.id\n" +
            "INNER JOIN parcels_rad_ead pre ON pre.id = pmd.id_parcels_rad_ead\n" +
            "INNER JOIN parcels p ON p.id = pre.parcel_id\n" +
            "WHERE pm.id = ? \n" +
            "   AND pm.status IN ("
            + PARCEL_MANIFEST_STATUS.OPEN.ordinal() +", "+PARCEL_MANIFEST_STATUS.CLOSE.ordinal() +") \n" +
            "   AND pmd.status NOT IN ("+ PARCEL_MANIFEST_DETAIL_STATUS.CANCELED.ordinal() +");";

    private static final String QUERY_GET_PACKAGES_BY_PARCEL = "SELECT\n" +
            "   pp.*\n" +
            "FROM parcels_manifest pm\n" +
            "INNER JOIN parcels_manifest_detail pmd ON pmd.id_parcels_manifest = pm.id\n" +
            "INNER JOIN parcels_rad_ead pre ON pre.id = pmd.id_parcels_rad_ead\n" +
            "INNER JOIN parcels p ON p.id = pre.parcel_id\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = p.id\n" +
            "WHERE pm.id = ?\n" +
            "   AND p.id = ?\n" +
            "    AND pmd.status IN("+ PARCEL_MANIFEST_DETAIL_STATUS.OPEN.ordinal() + ", "+ PARCEL_MANIFEST_DETAIL_STATUS.DELIVERED.ordinal() + ", " + PARCEL_MANIFEST_DETAIL_STATUS.NOT_DELIVERED.ordinal()+")\n" +
            "    AND p.parcel_status IN("+ PARCEL_STATUS.EAD.ordinal() +", "+ PARCEL_STATUS.ARRIVED.ordinal() +", "+ PARCEL_STATUS.ARRIVED_INCOMPLETE.ordinal() +","+ PARCEL_STATUS.DELIVERED_OK.ordinal() +")\n" +
            "    AND pp.package_status IN("+ PACKAGE_STATUS.ARRIVED.ordinal() +", "+ PACKAGE_STATUS.EAD.ordinal() +", "+ PACKAGE_STATUS.DELIVERED.ordinal() +")\n" +
            "    GROUP BY pp.id";

    private static final String QUERY_GET_ADDRESS_INFO_BY_PARCEL = "SELECT\n" +
            "   TRUE AS is_sender,\n" +
            "   ca.*\n" +
            "FROM parcels p\n" +
            "INNER JOIN customer_addresses ca ON ca.id = p.sender_address_id\n" +
            "WHERE p.id = ?\n" +
            "UNION ALL\n" +
            "SELECT\n" +
            "   FALSE AS is_sender,\n" +
            "   ca.*\n" +
            "FROM parcels p\n" +
            "INNER JOIN customer_addresses ca ON ca.id = p.addressee_address_id\n" +
            "WHERE p.id = ?";
}
