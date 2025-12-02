package database.parcel.handlers.ParcelDBV;

import database.commons.DBHandler;
import database.parcel.ParcelDBV;
import database.parcel.enums.PARCELPACKAGETRACKING_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import database.parcel.enums.SHIPMENT_TYPE;
import database.parcel.handlers.ParcelDBV.models.TrackingMessage;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static service.commons.Constants.*;

public class ExtendedTracking extends DBHandler<ParcelDBV> {

    public ExtendedTracking(ParcelDBV dbVerticle) {
            super(dbVerticle);
        }

    @Override
    public void handle(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String parcelTrackingCode = body.getString(_PARCEL_TRACKING_CODE);
            dbClient.queryWithParams(GET_PARCEL_TRACKING, new JsonArray().add(parcelTrackingCode), reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    JsonArray tracking = makeExtendedTracking(result);
                    message.reply(tracking);
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private JsonArray makeExtendedTracking(List<JsonObject> trackingList) {
        for (JsonObject tracking : trackingList) {
            PARCELPACKAGETRACKING_STATUS trackingAction = PARCELPACKAGETRACKING_STATUS.fromValue(tracking.getString(_ACTION));
            SHIPMENT_TYPE shipmentType = SHIPMENT_TYPE.fromValue(tracking.getString(_SHIPMENT_TYPE));
            String notes = tracking.getString(_NOTES);
            String eadDriverFullName = tracking.getString("ead_driver_full_name");
            switch (trackingAction) {
                case IN_TRANSIT:
                    tracking
                        .put(_ICON, "PiTruckTrailer")
                        .put(_MESSAGE, new JsonObject()
                            .put("es", new TrackingMessage(
                                    "En camino",
                                    "Tu paquete está en tránsito").toJsonObject())
                            .put("en", new TrackingMessage(
                                    "On way",
                                    "Your package is in transit").toJsonObject()));
                    break;
                case LOADED:
                    // Acción para LOADED
                    break;
                case DOWNLOADED:
                    // Acción para DOWNLOADED
                    break;
                case INCIDENCE:
                    // Acción para INCIDENCE
                    break;
                case CANCELED:
                    tracking
                        .put(_ICON, "TbClockCancel")
                        .put(_MESSAGE, new JsonObject()
                            .put("es", new TrackingMessage(
                                    "Cancelado",
                                    "Tu envío fue cancelado").toJsonObject())
                            .put("en", new TrackingMessage(
                                    "Canceled",
                                    "Your shipment was canceled").toJsonObject()));
                    break;
                case CLOSED:
                    // Acción para CLOSED
                    break;
                case DELIVERED:
                    tracking
                        .put(_ICON, "FaRegCircleCheck")
                        .put(_MESSAGE, new JsonObject()
                            .put("es", new TrackingMessage(
                                    "Entregado",
                                    "Tu paquete fue entregado").toJsonObject())
                            .put("en", new TrackingMessage(
                                    "Delivered",
                                    "Your package was delivered").toJsonObject()));
                    break;
                case DELIVERED_CANCEL:
                    // Acción para DELIVERED_CANCEL
                    break;
                case LOCATED:
                    // Acción para LOCATED
                    break;
                case ARRIVED:
                    String esMessage = shipmentType.includeEAD() ?
                            "Tu paquete llegó a sucursal y lo enviaremos a tu domicilio" :
                            "Tu paquete esta listo para su entrega";
                    String enMessage = shipmentType.includeEAD() ?
                            "Your package has arrived at our facility and is on its way to your address" :
                            "Your package is ready for delivery";

                    tracking
                        .put(_ICON, "TbBuildingWarehouse")
                        .put(_MESSAGE, new JsonObject()
                            .put("es", new TrackingMessage(
                                    "En sucursal destino",
                                    esMessage).toJsonObject())
                            .put("en", new TrackingMessage(
                                    "In branch",
                                    enMessage).toJsonObject()));
                    break;
                case EAD:
                    tracking
                        .put(_ICON, "TbTruckDelivery")
                        .put(_MESSAGE, new JsonObject()
                            .put("es", new TrackingMessage(
                                    "Rumbo a tu domicilio",
                                    "Tu paquete está en el ultimo tramo del recorrido",
                                    Objects.nonNull(eadDriverFullName) ? eadDriverFullName + " te entregará tu paquete" : null).toJsonObject())
                            .put("en", new TrackingMessage(
                                    "To your home",
                                    "Your package is in the last part of the journey",
                                    Objects.nonNull(eadDriverFullName) ? (eadDriverFullName + " will deliver your package to you") : null).toJsonObject()));
                    break;
                case READY_TO_TRANSHIPMENT:
                    tracking
                        .put(_ICON, "LiaPeopleCarrySolid")
                        .put(_MESSAGE, new JsonObject()
                            .put("es", new TrackingMessage(
                                "Trasbordando",
                                "Llegó a sucursal y esta listo para continuar su viaje").toJsonObject())
                            .put("en", new TrackingMessage(
                                    "Transshipping",
                                    "Arrived at the branch and ready to continue its journey").toJsonObject()));
                    break;
                case PENDING_COLLECTION:
                    tracking
                        .put(_ICON, "LuClipboardCheck")
                        .put(_MESSAGE, new JsonObject()
                            .put("es", new TrackingMessage(
                                    "Recibimos tu solicitud",
                                    "Pronto nos dirigiremos por tu paquete").toJsonObject())
                            .put("en", new TrackingMessage(
                                    "We receive your request",
                                    "We will be on our way to pick up your package soon").toJsonObject()));
                    break;
                case COLLECTING:
                    tracking
                        .put(_ICON, "FaTruckArrowRight")
                        .put(_MESSAGE, new JsonObject()
                            .put("es", new TrackingMessage(
                                    "En recolección",
                                    "Nos dirigimos por tu paquete").toJsonObject())
                            .put("en", new TrackingMessage(
                                    "Collecting",
                                    "We are headed for your package").toJsonObject()));
                    break;
                case COLLECTED:
                    tracking
                        .put(_ICON, "GiHandTruck")
                        .put(_MESSAGE, new JsonObject()
                            .put("es", new TrackingMessage(
                                    "Recolectado",
                                    "Tu paquete fue recolectado y se dirige a sucursal").toJsonObject())
                            .put("en", new TrackingMessage(
                                    "Collected",
                                    "Your package has been picked up and is on its way to the branch").toJsonObject()));
                    break;
                case IN_ORIGIN:
                    tracking
                        .put(_ICON, "LiaTruckLoadingSolid")
                        .put(_MESSAGE, new JsonObject()
                            .put("es", new TrackingMessage(
                                    "En preparación",
                                    "Tu paquete está siendo procesado para su salida").toJsonObject())
                            .put("en", new TrackingMessage(
                                    "In preparation",
                                    "Your package is being processed for departure").toJsonObject()));
                    break;
                case DELIVERY_ATTEMPT:
                    tracking
                        .put(_ICON, "TbUserExclamation")
                        .put(_MESSAGE, new JsonObject()
                            .put("es", new TrackingMessage(
                                    "Intento de entrega",
                                    "Intentamos entregar tu paquete, volveremos pronto",
                                    notes).toJsonObject())
                            .put("en", new TrackingMessage(
                                    "Delivery Attempt",
                                    "We tried to deliver your package, we will be back soon",
                                    notes).toJsonObject()));
                    break;
            }
        }

        if (!trackingList.isEmpty()) {
            JsonObject baseTracking = trackingList.get(0);
            PARCEL_STATUS parcelStatus = PARCEL_STATUS.values()[baseTracking.getInteger(_PARCEL_STATUS)];
            String promiseDeliveryDate = baseTracking.getString(_PROMISE_DELIVERY_DATE);
            SHIPMENT_TYPE shipmentType = SHIPMENT_TYPE.fromValue(baseTracking.getString(_SHIPMENT_TYPE));
            String terminalDestiny = baseTracking.getString("terminal_destiny");
            if ((parcelStatus.wasArrived() && shipmentType.includeEAD()) || !parcelStatus.wasDelivered() && !parcelStatus.wasCanceled()) {
                trackingList.add(new JsonObject()
                        .put(CREATED_AT, promiseDeliveryDate)
                        .put(_ACTION, _PROMISE_DELIVERY_DATE)
                        .put(_TERMINAL, shipmentType.includeEAD() ? "Entrega a domicilio" : terminalDestiny)
                        .put(_ICON, "IoMdStopwatch")
                        .put(_MESSAGE, new JsonObject()
                            .put("es", new TrackingMessage(
                                "Compromiso de entrega",
                                "Pronto tendrás el paquete en tus manos. Fecha estimada de entrega ").toJsonObject())
                            .put("en", new TrackingMessage(
                                "Promise delivery date",
                                "You will soon have the package in your hands. Promise delivery date ").toJsonObject())));
            }
        }

        return new JsonArray(trackingList.stream().filter(t -> t.containsKey(_MESSAGE)).collect(Collectors.toList()));
    }

    private static final String GET_PARCEL_TRACKING = "SELECT\n" +
            "    p.parcel_tracking_code, p.waybill, p.parcel_status,\n" +
            "    p.promise_delivery_date, p.shipment_type,\n" +
            "    CONCAT(b.name, ' (', b.prefix, ')') AS terminal,\n" +
            "    CONCAT(bd.name, ' (', bd.prefix, ')') AS terminal_destiny,\n" +
            "    ppt.action,\n" +
            "    ppt.notes,\n" +
            "    ppt.created_at,\n" +
            "    t.name AS trailer_name,\n" +
            "    CONCAT(e.name, ' ', e.last_name) AS driver_full_name,\n" +
            "    pm.drive_name AS ead_driver_full_name\n" +
            "FROM parcels_packages_tracking ppt\n" +
            "INNER JOIN parcels p ON p.id = ppt.parcel_id\n" +
            "INNER JOIN branchoffice b ON b.id = ppt.terminal_id\n" +
            "INNER JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            "LEFT JOIN shipments_parcel_package_tracking sppt ON sppt.parcel_id = p.id\n" +
            "LEFT JOIN shipments s ON s.id = sppt.shipment_id\n" +
            "LEFT JOIN trailers t ON t.id = sppt.trailer_id\n" +
            "LEFT JOIN employee e ON e.id = s.driver_id\n" +
            "LEFT JOIN parcels_manifest pm ON pm.id = ppt.parcel_manifest_id\n" +
            "WHERE p.parcel_tracking_code = ?\n" +
            "AND ppt.action NOT IN ('register', 'paid', 'incidence', 'printed')\n" +
            "GROUP BY ppt.parcel_id, ppt.action, ppt.terminal_id, ppt.notes, pm.id\n" +
            "ORDER BY ppt.created_at;";

}