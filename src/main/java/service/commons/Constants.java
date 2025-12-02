/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.commons;

/**
 * Class to store global constants
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class Constants {

    public static final String CONFIG_HTTP_SERVER_PORT = "httpServerPort";
    public static final String WEB_SERVER_HOSTNAME = "web_server_hostname";
    public static final String ACTION = "action";
    public static final String GOOGLE_MAPS_API_KEY = "google_maps_api_key";
    public static final String INVALID_DATA = "Invalid data";
    public static final String INVALID_DATA_MESSAGE = "Some properties in the model are invalid, see details in data";
    public static final String UNEXPECTED_ERROR = "An unexpected error occurred, check with the systems provider";
    public static final String AUTHORIZATION = "authorization";
    public static final String STATUS = "status";
    public static final String USER_ID = "user_id";
    public static final String ID = "id";
    public static final String EMPLOYEE = "employee";
    public static final String EMPLOYEE_ID = "employee_id";
    public static final String CUSTOMER_ID = "customer_id";
    public static final String CASHOUT_ID = "cash_out_id";
    public static final String CASH_REGISTER_ID = "cash_register_id";
    public static final String INTEGRATION_PARTNER_SESSION_ID = "integration_partner_session_id";
    public static final String CREATED_BY = "created_by";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_BY = "updated_by";
    public static final String UPDATED_AT = "updated_at";
    public static final String AMOUNT = "amount";
    public static final String DISCOUNT = "discount";
    public static final String TOTAL_AMOUNT = "total_amount";
    public static final String IVA = "iva";
    public static final String PREVIOUS_DISCOUNT = "previous_discount";
    public static final String DISCOUNT_APPLIED = "discount_applied";
    public static final String INTERNAL_CUSTOMER = "internal_customer";
    public static final String INTERNAL_PARCEL = "is_internal_parcel";
    public static final String VALUE = "value";
    public static final String INSURANCE_AMOUNT = "insurance_amount";
    public static final String EXTRA_CHARGES = "extra_charges";
    public static final String PACKAGES = "packages";
    public static final String TOTAL = "total";
    public static final String TOKEN = "token";
    public static final String IP = "ip";
    public static final String APLICABLE = "aplicable";
    public static final String NOT_APLICABLE = "not_aplicable";
    public static final String CANCELED_AT = "canceled_at";
    public static final String CANCELED_BY = "canceled_by";
    public static final String NOTES = "notes";
    public static final String BRANCHOFFICE_ID = "branchoffice_id";
    public static final String TIME = "time";
    public static final String NEW_TICKET = "new_ticket";
    public static final String DETAIL = "detail";
    public static final String UNIT_PRICE = "unit_price";
    public static final String QUANTITY = "quantity";
    public static final String TERMINAL_ID = "terminal_id";
    public static final String DATE = "date";
    public static final String REPORT = "report";
    public static final String LIMIT = "limit";
    public static final String PAGE = "page";
    public static final String ITEMS = "items";
    public static final String RESULTS = "results";
    public static final String BOARDINGPASS_ID = "boardingpass_id";
    public static final String TICKET_TYPE = "ticket_type";
    public static final String TICKET_TYPE_ROUTE = "ticket_type_route";
    public static final String _ORIGIN = "origin";
    public static final String ORIGIN = "origin";
    public static final String DESTINY = "destiny";
    public static final String TERMINAL_ADDRESS = "terminal_address";
    public static final String DESTINIES = "destinies";
    public static final String _SHIPMENT_TYPE = "shipment_type";
    public static final String _SCHEDULE_STATUS = "schedule_status";
    public static final String _DESTINATION_STATUS = "destination_status";
    public static final String LATEST_MOVEMENT = "latest_movement";
    public static final String _TERMINAL_ORIGIN_ID = "terminal_origin_id";
    public static final String _TERMINAL_DESTINY_ID = "terminal_destiny_id";
    public static final String _TERMINAL_DESTINY_PREFIX = "terminal_destiny_prefix";
    public static final String _PARCELS = "parcels";
    public static final String _PARCEL_PACKAGES = "parcel_packages";
    public static final String _PARCEL_PACKINGS = "parcel_packings";
    public static final String _PRICE = "price";
    public static final String _DISTANCE_KM = "distance_km";
    public static final String _COST = "cost";
    public static final String _WEIGHT = "weight";
    public static final String _WIDTH = "width";
    public static final String _HEIGHT = "height";
    public static final String _LENGTH = "length";
    public static final String _LINEAR_VOLUME = "linear_volume";
    public static final String _PACKING_ID = "packing_id";
    public static final String _SHIPPING_TYPE = "shipping_type";
    public static final String _CONTAINS = "contains";
    public static final String _QUANTITY = "quantity";
    public static final String _CUSTOMER_ID = "customer_id";
    public static final String _SERVICE = "service";
    public static final String _AMOUNT = "amount";
    public static final String _DISCOUNT = "discount";
    public static final String _TOTAL_AMOUNT = "total_amount";
    public static final String _BASE_PACKAGE_PRICE_ID = "base_package_price_id";
    public static final String _BASE_PACKAGE_PRICE_NAME = "base_package_price_name";
    public static final String _MAX_WEIGHT = "max_weight";
    public static final String _MAX_M3 = "max_m3";
    public static final String _EXCESS_COST = "excess_cost";
    public static final String _PRICE_KG = "price_kg";
    public static final String _PRICE_CUBIC = "price_cubic";
    public static final String _PACKAGE_PRICE_KM_ID = "package_price_km_id";
    public static final String _PACKAGE_PRICE_ID = "package_price_id";
    public static final String _EXCESS_BY = "excess_by";
    public static final String _VOLUME = "volume";
    public static final String _NAME_PRICE = "name_price";
    public static final String _MAX_PACKAGE_PRICE_NAME_APPLICABLE = "max_package_price_name_applicable";
    public static final String _MAX_PACKAGE_PRICE_ID_APPLICABLE = "max_package_price_id_applicable";
    public static final String _DISCOUNT_TYPE = "discount_type";
    public static final String _PACKAGE_TYPE_ID = "package_type_id";
    public static final String _TYPE_SERVICE = "type_service";
    public static final String _IVA = "iva";
    public static final String _COST_BREAKDOWN = "cost_breakdown";
    public static final String _APPLY_RAD = "apply_rad";
    public static final String _APPLY_EAD = "apply_ead";
    public static final String _APPLY_SENDER_ADDRESSEE = "apply_sender_addressee";
    public static final String _DISCOUNT_CODE = "discount_code";
    public static final String _NAME = "name";
    public static final String _TITLE = "title";
    public static final String _DESCRIPTION = "description";
    public static final String _INSURANCE_AMOUNT_BEFORE_IVA = "insurance_amount_before_iva";
    public static final String _INSURANCE_AMOUNT_IVA = "insurance_amount_iva";
    public static final String _EXCESS_AMOUNT = "excess_amount";
    public static final String _EXCESS_DISCOUNT = "excess_discount";
    public static final String _SERVICES_RAD_AMOUNT = "services_rad_amount";
    public static final String _SERVICES_EAD_AMOUNT = "services_ead_amount";
    public static final String _UNIT_PRICE = "unit_price";
    public static final String _EXTRA_CHARGES = "extra_charges";
    public static final String _SERVICES_AMOUNT = "services_amount";
    public static final String _INSURANCE_AMOUNT = "insurance_amount";
    public static final String _ID_TYPE_SERVICE = "id_type_service";
    public static final String _ZIP_CODE_SERVICE = "zip_code_service";
    public static final String _PARCEL_IVA = "parcel_iva";
    public static final String _PROMO_APPLIED_INFO = "promo_applied_info";
    public static final String _IS_DISCOUNT_BY_EXCESS = "is_discount_by_excess";
    public static final String _EXCESS_PROMO_ID = "excess_promo_id";
    public static final String _MORAL = "moral";
    public static final String _FISICO = "fisico";
    public static final String _AMOUNT_BEFORE_IVA = "amount_before_iva";
    public static final String _EXCESS_AMOUNT_BEFORE_IVA = "excess_amount_before_iva";
    public static final String _DISCOUNT_BEFORE_IVA = "discount_before_iva";
    public static final String _EXCESS_DISCOUNT_BEFORE_IVA = "excess_discount_before_iva";
    public static final String _SERVICES_AMOUNT_BEFORE_IVA = "services_amount_before_iva";
    public static final String _TOTAL_PARCEL_IVA = "total_parcel_iva";
    public static final String _CUSTOMER_BILLING_INFORMATION_ID = "customer_billing_information_id";

    public static final String _PURCHASE_ORIGIN = "purchase_origin";
    public static final String _RFC = "rfc";
    public static final String _EMPLOYEE_ID = "employee_id";
    public static final String _SENDER_ID = "sender_id";
    public static final String _SENDER_INFO = "sender_info";
    public static final String _ADDRESSEE_ID = "addressee_id";
    public static final String _ADDRESSEE_INFO = "addressee_info";
    public static final String _PAYS_SENDER = "pays_sender";

    public static final String PARCEL_AMOUNT_COLUMN_DEF = "(p.amount + p.excess_amount + p.services_amount + p.insurance_amount + p.extra_charges) - (p.discount + p.excess_discount)";
    public static final String PARCEL_FREIGHT_COLUMN_DEF = "(p.amount + p.excess_amount) - (p.discount + p.excess_discount)";
    public static final String CCP_PREFIX = "cartaporte31";
    public static final String PC_PREFIX = "pago20";

    public static final String CCP_VERSION = "3.1";
    public static final String _PACKAGE = "package";
    public static final String _MESSAGE = "message";
    public static final String _CAUSE = "cause";
    public static final String _ES = "es";
    public static final String _EN = "en";
    public static final String _PARCEL_ID = "parcel_id";
    public static final String _PARCEL_PACKAGE_ID = "parcel_package_id";
    public static final String _SCHEDULE_ROUTE_ID = "schedule_route_id";
    public static final String _TRAILER_ID = "trailer_id";
    public static final String _BRANCHOFFICE_ID = "branchoffice_id";
    public static final String _SHIPMENT_ID = "shipment_id";
    public static final String _ACTION = "action";
    public static final String _TERMINAL_ID = "terminal_id";
    public static final String _COUNT_LOAD = "count_load";
    public static final String _TOTAL_PACKAGES = "total_packages";
    public static final String _ORDER_DESTINY = "order_destiny";
    public static final String _INIT_DATE = "init_date";
    public static final String _END_DATE = "end_date";
    public static final String _TYPE = "type";
    public static final String _INVOICE_VALUE = "invoice_value";
    public static final String _PARCEL_TRACKING_CODE = "parcel_tracking_code";
    public static final String _PACKAGE_CODES = "package_codes";
    public static final String _SHIPMENT_STATUS = "shipment_status";
    public static final String _PARCEL_STATUS = "parcel_status";
    public static final String _SHIPMENT_PARCEL_ID = "shipment_parcel_id";
    public static final String _ECONOMIC_NUMBER = "economic_number";
    public static final String _LAST_PARCEL_STATUS = "last_parcel_status";
    public static final String _LAST_PACKAGE_STATUS = "last_package_status";
    public static final String _PACKAGE_STATUS = "package_status";
    public static final String _PACKAGE_CODE = "package_code";
    public static final String _TRANSHIPMENT_COUNT = "transhipment_count";
    public static final String _FROM_TRAILER_ID = "from_trailer_id";
    public static final String _TO_TRAILER_ID = "to_trailer_id";
    public static final String _PROMISE_TIME_OCU = "promise_time_ocu";
    public static final String _PROMISE_TIME_EAD = "promise_time_ead";
    public static final String _PROMISE_DELIVERY_DATE = "promise_delivery_date";
    public static final String _IS_PENDING_COLLECTION = "is_pending_collection";
    public static final String _NOTES = "notes";
    public static final String _USER = "user";
    public static final String _PROFILE = "profile";
    public static final String _IS_CONTINGENCY = "is_contingency";
    public static final String _PACKAGES = "packages";
    public static final String _IS_VIRTUAL = "is_virtual";
    public static final String _SECOND_TRAILER_ID = "second_trailer_id";
    public static final String _SECOND_LEFT_STAMP = "second_left_stamp";
    public static final String _SECOND_RIGHT_STAMP = "second_right_stamp";
    public static final String _SECOND_ADDITIONAL_STAMP = "second_additional_stamp";
    public static final String _SECOND_REPLACEMENT_STAMP = "second_replacement_stamp";
    public static final String _SECOND_FIFTH_STAMP = "second_fifth_stamp";
    public static final String _SECOND_SIXTH_STAMP = "second_sixth_stamp";
    public static final String _FIFTH_STAMP = "fifth_stamp";
    public static final String _SIXTH_STAMP = "sixth_stamp";
    public static final String _TRAVEL_LOGS_ID = "travel_logs_id";
    public static final String _PROMISE_TIME = "promise_time";
    public static final String _ADDITIONAL_PROMISE_TIME = "additional_promise_time";
    public static final String _PARCEL_PREPAID_TOTAL_AMOUNT = "parcel_prepaid_total_amount";
    public static final String _TOTAL_COUNT_GUIPP = "total_count_guipp";
    public static final String _IVA_PERCENT = "iva_percent";
    public static final String _REAL_PERCENT_DISCOUNT_APPLIED = "real_percent_discount_applied";
    public static final String _PERCENT_DISCOUNT_APPLIED = "percent_discount_applied";
    public static final String _MAX_COST = "max_cost";
    public static final String _PARCEL_PREPAID_ID = "parcel_prepaid_id";
    public static final String _CODES = "codes";
    public static final String _PARCEL_PREPAID_DETAIL_ID = "parcel_prepaid_detail_id";
    public static final String _DETAILS = "details";
    public static final String _DETAIL = "detail";
    public static final String _CANCEL_PARCEL_PACKAGE_ID = "cancel_parcel_package_id";
    public static final String _INSURANCE_VALUE = "insurance_value";
    public static final String _INSURANCE_ID = "insurance_id";
    public static final String _INSURANCE_PERCENT = "insurance_percent";
    public static final String _MAX_INSURANCE_VALUE = "max_insurance_value";
    public static final String _INSURANCE = "insurance";
    public static final String _VEHICLE_ID = "vehicle_id";
    public static final String _VEHICLE_NAME = "vehicle_name";
    public static final String _TYPE_SERVICE_ID = "type_service_id";
    public static final String _CASH_OUT = "cash_out";
    public static final String _INITIAL_FUND = "initial_fund";
    public static final String _PARCEL_MANIFEST_ID = "parcel_manifest_id";
    public static final String _PARCEL_RAD_EAD_ID = "parcel_rad_ead_id";
    public static final String _FINISH_LOAD_DATE = "finish_load_date";
    public static final String _INIT_ROUTE_DATE = "init_route_date";
    public static final String _FINISH_ROUTE_DATE = "finish_route_date";
    public static final String _CONTINGENCY_FINISH_ROUTE_DATE = "contingency_finish_route_date";
    public static final String _PARCEL_MANIFEST_DETAIL_ID = "parcel_manifest_detail_id";
    public static final String _PARCELS_DELIVERIES_ID = "parcels_deliveries_id";
    public static final String _CITY_ID = "city_id";
    public static final String _ID_REASON_NO_RAD_EAD = "id_reason_no_rad_ead";
    public static final String _DELIVERY_ATTEMPT_REASON_ID = "delivery_attempt_reason_id";
    public static final String _OTHER_REASONS_NOT_RAD_EAD = "other_reasons_not_rad_ead";
    public static final String _PARCEL_MANIFEST_STATUS = "parcel_manifest_status";
    public static final String _PARCEL_MANIFEST_DETAIL_STATUS = "parcel_manifest_detail_status";
    public static final String _IMAGE_NAME = "image_name";
    public static final String _FIRST_NOTE = "first_note";
    public static final String _SECOND_NOTE = "second_note";
    public static final String _TERMINAL = "terminal";
    public static final String _ICON = "icon";
    public static final String _SPEED = "speed";
    public static final String _LATITUDE = "latitude";
    public static final String _LONGITUDE = "longitude";
    public static final String _USER_ID = "user_id";

    public enum BOARDING_PASS_STATUS {
        CANCELED(0),
        PAID(1),
        IN_TRANSIT(2),
        FINISHED(3),
        PRE_BOARDING(4),
        PENDING_RETURN(5),
        EXPIRED(6);

        Integer value;

        BOARDING_PASS_STATUS(Integer i) {
        this.value = i;
        }

        public Integer getValue() {
            return value;
        }
    }

    public enum TICKET_TYPES {
        ABIERTO_REDONDO("abierto_redondo"),
        REDONDO("redondo"),
        SENCILLO("sencillo"),
        ABIERTO_SENCILLO("abierto_sencillo");

        String value;

        TICKET_TYPES(String i) {
            this.value = i;
        }

        public String getValue() {
            return value;
        }
    }
}
