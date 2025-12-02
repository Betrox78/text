/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.commons;

import com.google.maps.GeoApiContext;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.SQLOptions;
import utils.UtilsDate;
import utils.UtilsValidation;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static database.promos.PromosDBV.DISCOUNT;
import static java.util.stream.Collectors.toList;
import static service.commons.Constants.*;

/**
 * Base Verticle for LCRUD entities, this class works as a facade
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public abstract class DBVerticle extends AbstractVerticle {

    /**
     * the sql client contains the channel of comunication with the database
     */
    protected SQLClient dbClient;

    protected GeoApiContext geoApiContext;


    static private int MAX_LIMIT = 100;

    /**
     * method that runs when the verticles is deployed
     *
     * @param startFuture future to start with this deployment
     * @throws Exception
     */
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        JsonObject conf = new JsonObject()
                .put("url", config().getString("url"))
                .put("driver_class", config().getString("driver_class"))
                .put("user", config().getString("user"))
                .put("password", config().getString("password"))
                .put("max_pool_size", config().getInteger("max_pool_size"))
                .put("max_pool_size", config().getInteger("max_pool_size"))
                .put("min_pool_size", config().getInteger("min_pool_size"))
                .put("initial_pool_size", config().getInteger("initial_pool_size"))
                .put("max_statements", config().getInteger("max_statements"))
                .put("max_statements_per_connection", config().getInteger("max_statements_per_connection"))
                .put("max_idle_time", config().getInteger("max_idle_time"));

        dbClient = JDBCClient.createShared(vertx, conf);

        dbClient.getConnection(ar -> {
            if (ar.failed()) {
                System.out.println("Could not open a database connection" + ar.cause());
                startFuture.fail(ar.cause());
            } else {
                vertx.eventBus().consumer(this.getClass().getSimpleName(), this::onMessage);
                startFuture.complete();
                ar.result().close();//close de connection, it will not be used
            }
        });

        geoApiContext = new GeoApiContext.Builder().apiKey(config().getString(GOOGLE_MAPS_API_KEY)).build();

    }

    /**
     * This method takes the action of the message and execute the method that corresponds
     *
     * @param message the message from the event bus
     */
    protected void onMessage(Message<JsonObject> message) {
        if (isValidAction(message)) {
            try {
                Action action = Action.valueOf(message.headers().get(ACTION));
                switch (action) {
                    case CREATE:
                        this.create(message);
                        break;
                    case DELETE_BY_ID:
                        this.deleteById(message);
                        break;
                    case FIND_BY_ID:
                        this.findById(message);
                        break;
                    case FIND_ALL:
                        this.findAll(message);
                        break;
                    case FIND_ALL_V2:
                        this.findAllV2(message);
                        break;
                    case UPDATE:
                        this.update(message);
                        break;
                    case COUNT:
                        this.count(message);
                        break;
                    case TRANSACTION_LOGGER:
                        this.transactionLogger(message);
                        break;
                    case EXCEPTION_LOGGER:
                        this.exceptionLogger(message);
                        break;
                }
            } catch (IllegalArgumentException e) {
            }
        }
    }

    protected CompletableFuture<JsonObject> internalCustomerValidation(Boolean flagInternalCustomer,Boolean isInternalParcel, JsonObject body){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            if (flagInternalCustomer && isInternalParcel){
                Double insuranceAmount = body.getDouble(INSURANCE_AMOUNT, 0.00);
                Double extraCharges = body.getDouble(EXTRA_CHARGES, 0.00);
                Double amount = body.getDouble(AMOUNT);
                body.put(DISCOUNT, amount + insuranceAmount + extraCharges);
                body.put(TOTAL_AMOUNT, 0.00);
                future.complete(body);
            } else {
                future.complete(body);
            }
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<String> generateCompuestID(String service, Integer cashRegisterId, Boolean isInternalCustomer) {
        return this.generateCompuestID(service, cashRegisterId, isInternalCustomer, "sucursal");
    }

    public CompletableFuture<String> generateCompuestID(String service, Integer cashRegisterId, Boolean isInternalCustomer, String purchaseOrigin){
        CompletableFuture<String> future = new CompletableFuture<>();
        this.getQuantityService(service, cashRegisterId, true, purchaseOrigin).whenComplete((resultQuantityService, errorQuantityService) -> {
            try{
                if (errorQuantityService != null){
                    throw errorQuantityService;
                }
                future.complete(
                        this.getCompuestId(
                                resultQuantityService.getString("branchoffice_prefix"),
                                resultQuantityService.getString("cash_register_prefix"),
                                resultQuantityService.getInteger("quantity"), isInternalCustomer));
            } catch (Throwable t){
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private String getCompuestId(String branchofficePrefix, String cashOutPrefix, Integer quantityService, Boolean isInternalCustomer){
        String consecutiveNumber = String.format("%07d", quantityService);
        if(isInternalCustomer){
            branchofficePrefix = branchofficePrefix.concat("IN");
        }
        return branchofficePrefix+cashOutPrefix+consecutiveNumber;
    }

    private CompletableFuture<JsonObject> getQuantityService(String service, Integer cashRegisterId, Boolean flagGroupBy, String purchaseOrigin){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String QUERY = QUERY_GET_QUANTITY_SERVICE_BY_BRANCH_AND_CASH_OUT;
        JsonArray params = new JsonArray().add(cashRegisterId);

        if ("web service".equals(purchaseOrigin)) {
            if ("parcel".equals(service)) {
                QUERY = QUERY_GET_QUANTITY_PARCEL_SERVICE_BY_INTEGRATION_PARTNER_ID;
            } else {
                future.completeExceptionally(new Throwable("The service has not been specified"));
            }
        } else {
            switch (service){
                case "parcel":
                    QUERY = QUERY_GET_QUANTITY_PARCEL_SERVICE_BY_BRANCH_AND_CASH_REGISTER_ID;
                    break;
                case "boardingpass":
                    QUERY = QUERY.concat(PARAM_BOARDINGPASS_GET_QUANTITY_SERVICE_BY_BRANCH_AND_CASH_OUT);
                    break;
                case "rental":
                    QUERY = QUERY.concat(PARAM_RENTAL_GET_QUANTITY_SERVICE_BY_BRANCH_AND_CASH_OUT);
                    break;
                case "prepaid":
                    QUERY = QUERY.concat(QUERY_GET_QUANTITY_PREPAID_SERVICE_BY_BRANCH_AND_CASH_REGISTER_ID);
                    break;
                case "guiapp":
                    QUERY = QUERY.concat(PARAM_GUIAPP_GET_QUANTITY_SERVICE_BY_BRANCH_AND_CASH_REGISTER_ID);
                default:
                    future.completeExceptionally(new Throwable("The service has not been specified"));
                    break;
            }
        }

        QUERY = flagGroupBy ? QUERY.concat(GET_QUANTITY_SERVICE_GROUP_BY) : QUERY;

        this.dbClient.queryWithParams(QUERY, params, reply ->{
            try{
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> results = reply.result().getRows();
                if (results.isEmpty()){
                    this.getQuantityService(service, cashRegisterId, false, purchaseOrigin).whenComplete((result, error) -> {
                        try {
                            if (error != null){
                                throw error;
                            }
                            future.complete(result);
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
                } else {
                    JsonObject result = results.get(0);
                    if(result.getString("branchoffice_prefix") == null) {
                        throw new Exception("The branchoffice prefix is null");
                    }
                    if(result.getString("cash_register_prefix") == null){
                        throw new Exception("The cash register prefix is null");
                    }
                    future.complete(result);
                }
            } catch(Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private static final String QUERY_GET_QUANTITY_SERVICE_BY_BRANCH_AND_CASH_OUT = "SELECT DISTINCT\n" +
            " COUNT(t.id)+1 AS quantity,\n" +
            " cr.prefix AS cash_register_prefix, \n" +
            " b.prefix AS branchoffice_prefix \n" +
            "FROM tickets t \n" +
            "LEFT JOIN cash_out co ON co.id = t.cash_out_id \n" +
            "LEFT JOIN cash_registers cr ON cr.id = co.cash_register_id\n" +
            "LEFT JOIN branchoffice b ON b.id = cr.branchoffice_id\n" +
            "WHERE t.action = 'purchase'\n" +
            "AND cr.status = 1\n" +
            "AND cr.id = ? ";

    private static final String QUERY_GET_QUANTITY_PARCEL_SERVICE_BY_BRANCH_AND_CASH_REGISTER_ID = "SELECT\n" +
            " COUNT(p.id)+1 AS quantity,\n" +
            " cr.prefix AS cash_register_prefix, \n" +
            " b.prefix AS branchoffice_prefix\n" +
            "FROM cash_registers cr\n" +
            "LEFT JOIN parcels p ON p.cash_register_id = cr.id  \n" +
            "LEFT JOIN branchoffice b ON b.id = cr.branchoffice_id\n" +
            "WHERE cr.id = ? ";

    private static final String QUERY_GET_QUANTITY_PARCEL_SERVICE_BY_INTEGRATION_PARTNER_ID = "WITH selected_session AS (\n" +
            "  SELECT integration_partner_api_key_id\n" +
            "  FROM integration_partner_session\n" +
            "  WHERE id = ?\n" +
            ")\n" +
            "SELECT \n" +
            "  COUNT(p.id) + 1 AS quantity,\n" +
            "  cr.prefix AS cash_register_prefix,\n" +
            "  b.prefix AS branchoffice_prefix\n" +
            "FROM selected_session ss\n" +
            "JOIN integration_partner_api_key cr ON cr.id = ss.integration_partner_api_key_id\n" +
            "JOIN integration_partner b ON b.id = cr.integration_partner_id\n" +
            "LEFT JOIN parcels p ON p.integration_partner_session_id IN (\n" +
            "  SELECT id FROM integration_partner_session\n" +
            "  WHERE integration_partner_api_key_id = ss.integration_partner_api_key_id\n" +
            ") \n ";

    private static final String QUERY_GET_QUANTITY_PREPAID_SERVICE_BY_BRANCH_AND_CASH_REGISTER_ID = " AND t.prepaid_travel_id IS NOT NULL ";

    private static final String PARAM_GUIAPP_GET_QUANTITY_SERVICE_BY_BRANCH_AND_CASH_REGISTER_ID = "SELECT\n" +
            " COUNT(p.id)+1 AS quantity,\n" +
            " cr.prefix AS cash_register_prefix, \n" +
            " b.prefix AS branchoffice_prefix\n" +
            "FROM parcels_prepaid p\n" +
            "LEFT JOIN cash_registers cr ON cr.id = p.cash_register_id\n" +
            "LEFT JOIN branchoffice b ON b.id = cr.branchoffice_id\n" +
            "WHERE cr.id = ? ";

    private static final String PARAM_BOARDINGPASS_GET_QUANTITY_SERVICE_BY_BRANCH_AND_CASH_OUT = " AND t.boarding_pass_id IS NOT NULL ";
    private static final String PARAM_RENTAL_GET_QUANTITY_SERVICE_BY_BRANCH_AND_CASH_OUT = " AND t.rental_id IS NOT NULL ";
    private static final String GET_QUANTITY_SERVICE_GROUP_BY = " GROUP BY \n" +
            " cr.prefix \n" +
            " AND b.prefix;";

    protected CompletableFuture<Boolean> validateField(final String tableName, final String fieldName, final String searchValue){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        final String QUERY = "SELECT id, status FROM "+tableName+" WHERE "+fieldName+" = '"+searchValue+"' ORDER BY id DESC LIMIT 1";

        this.dbClient.query(QUERY, result -> {
            try {
                if (result.failed()){
                    throw  result.cause();
                }
                List<JsonObject> results = result.result().getRows();
                if (!results.isEmpty() && results.get(0).getInteger("status") != 3){
                    throw new Exception(searchValue+" in "+tableName+" already exists");
                }
                future.complete(true);
            } catch (Throwable t){
                t.printStackTrace();
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    protected CompletableFuture<JsonArray> execUpdatePrintsCounter(SQLConnection conn, JsonArray arrayReference, String tableName, Integer updatedBy){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(arrayReference.stream()
                .map(array -> updatePrintsCounter(conn, (JsonObject) array, tableName, updatedBy))
                .toArray(CompletableFuture[]::new))
                .whenComplete((result, error) -> {
                    try{
                        if (error != null) {
                            throw error;
                        }
                        future.complete(arrayReference);
                    } catch (Throwable t){
                        t.printStackTrace();
                        future.completeExceptionally(t);
                    }
                });
        return future;
    }

    protected CompletableFuture<JsonObject> updatePrintsCounter(SQLConnection conn, JsonObject print, String tableName, Integer updatedBy){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try{
            Integer id = print.getInteger("id");
            Integer printCounter = print.getInteger("prints_counter")+1;
            GenericQuery update = generateGenericUpdate(tableName, new JsonObject()
                    .put("id", id)
                    .put("prints_counter", printCounter)
                    .put("updated_by", updatedBy)
                    .put("updated_at", UtilsDate.sdfDataBase(new Date())));
            conn.updateWithParams(update.getQuery(), update.getParams(), updateHandler -> {
                try{
                    if (updateHandler.failed()){
                        throw updateHandler.cause();
                    }
                    print.put("prints_counter", printCounter);
                    future.complete(updateHandler.result().toJson());
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
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
                        Integer reprints = prints-1;
                        track.put("notes", "Reimpresión #"+reprints);
                    }
                } else {
                    track.put("notes", notes);
                }
                batch.add(generateGenericCreate(trackingTable, track));
            }
            conn.batch(batch, batchReply -> {
                try {
                    if (batchReply.failed()){
                        throw batchReply.cause();
                    }
                    future.complete(true);
                } catch (Throwable t){
                    t.printStackTrace();
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    /**
     * Validates if the action in the headers is valid
     *
     * @param message the message from the event bus
     * @return true if containg an action, false otherwise
     */
    protected boolean isValidAction(Message<JsonObject> message) {
        if (!message.headers().contains("action")) {
            message.fail(ErrorCodes.NO_ACTION_SPECIFIED.ordinal(), "No action header specified");
            return false;
        }
        return true;
    }

    /**
     * This methos has the behaivor for reporting an error in a query execute
     *
     * @param message the message from the eventu bus
     * @param cause the exception giving problems
     */
    protected final void reportQueryError(Message<JsonObject> message, Throwable cause) {
        try {
            JsonObject catchedError = null;
            if (cause.getMessage().contains("foreign key")) { //a foreign key constraint fails
                Pattern p = Pattern.compile("`(.+?)`");
                Matcher m = p.matcher(cause.getMessage());
                List<String> incidencias = new ArrayList<>(5);
                while (m.find()) {
                    incidencias.add(m.group(1));
                }
                catchedError = new JsonObject().put("name", incidencias.get(incidencias.size() - 3)).put("error", "does not exist in the catalog");
            }
            if (cause.getMessage().contains("Data too long")) { //data to long for column
                Pattern p = Pattern.compile("'(.+?)'");
                Matcher m = p.matcher(cause.getMessage());
                m.find();
                String propertyName = m.group(1);
                catchedError = new JsonObject().put("name", propertyName).put("error", "to long data");
            }
            if (cause.getMessage().contains("Unknown column")) {//unkown column
                Pattern p = Pattern.compile("'(.+?)'");
                Matcher m = p.matcher(cause.getMessage());
                m.find();
                String propertyName = m.group(1);
                catchedError = new JsonObject().put("name", propertyName).put("error", UtilsValidation.PARAMETER_DOES_NOT_EXIST);
            }
            if (cause.getMessage().contains("doesn't have a default")) { //not default value in not null
                Pattern p = Pattern.compile("'(.+?)'");
                Matcher m = p.matcher(cause.getMessage());
                m.find();
                String propertyName = m.group(1);
                catchedError = new JsonObject().put("name", propertyName).put("error", UtilsValidation.MISSING_REQUIRED_VALUE);
            }
            if (cause.getMessage().contains("Duplicate entry")) { //already exist (duplicate key for unique values)
                Pattern p = Pattern.compile("'(.+?)'");
                Matcher m = p.matcher(cause.getMessage());
                m.find();
                String value = m.group(1);
                m.find();
                String propertyName = m.group(1);
                catchedError = new JsonObject().put("name", propertyName).put("error", "value: " + value + " in " + UtilsValidation.ALREADY_EXISTS);
            }
            if (cause.getMessage().contains("Data truncation")) {
                Pattern p = Pattern.compile("'(.+?)'");
                Matcher m = p.matcher(cause.getMessage());
                m.find();
                String propertyName = m.group(1);
                catchedError = new JsonObject().put("name", propertyName).put("error", "to long data");
            }
            if (catchedError != null) {
                message.reply(catchedError, new DeliveryOptions().addHeader(ErrorCodes.DB_ERROR.toString(), catchedError.getString("error")));
            } else {
                message.fail(ErrorCodes.DB_ERROR.ordinal(), cause.getMessage());
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            message.fail(500, ex.getMessage());
        }

    }

    /**
     * Execute the query "select * from"
     *
     * @param message message from the event bus
     */
    protected void findAll(Message<JsonObject> message) {
        try {
            String queryParam = message.body().getString("query");
            String fromParam = message.body().getString("from");
            String toParam = message.body().getString("to");

            String queryToExcecute;
            if (queryParam != null) {
                queryToExcecute = this.select(queryParam);
            } else {
                queryToExcecute = "select * from " + this.getTableName() + " where status != " + Status.DELETED.getValue();
            }
            //adds the limit for pagination
            if (fromParam != null && toParam != null) {
                queryToExcecute += " limit " + fromParam + "," + toParam;
            } else if (toParam != null) {
                queryToExcecute += " limit " + toParam;
            }
            dbClient.query(queryToExcecute, reply -> {
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
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    /**
     * Execute the query to find all the elements of this database verticle
     *
     * @param message message from the event bus
     */
    protected void findAllV2(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String page = body.getString("page");
            String limit = body.getString("limit", Integer.toString(MAX_LIMIT));
            String selectParam = body.getString("select");
            String specialJoinParam = body.getString("specialJoin");
            String whereParam = body.getString("where");
            String joinParam = body.getString("joinType");
            String fromParam = body.getString("from");
            String toParam = body.getString("to");
            String search = body.getString("search");
            String searchKeys = body.getString("searchKeys");
            String orderBy = body.getString("orderBy");
            /*sets the join type to one valid INNER, RIGHT or LEFT*/
            joinParam = this.validateJoinParam(joinParam);
            Set<Join> joinsNeeded = new HashSet<>();
            /*add especial joins to joinsNeeded*/
            this.addSpecialJoins(specialJoinParam, joinsNeeded, joinParam);

            //create parts of the query
            String querySelect = this.selectV2(selectParam, joinsNeeded, joinParam);
            String queryFrom = " FROM " + this.getTableName() + " ";
            String queryWhere = this.whereV2(whereParam, joinsNeeded, joinParam);
            if (search != null && searchKeys != null) {
                List<String> keys = new ArrayList<>(Arrays.asList(searchKeys.split(",")));
                List<String> searches = new ArrayList<>(Arrays.asList(search.split(" ")));
                if (!keys.isEmpty()) {
                    String where = queryWhere;
                    List<String> searchParts = new ArrayList<>();
                    keys.forEach(key -> {
                        int i = key.indexOf('*');
                        String likeExpression = "";
                        if (i > -1) {
                            if (i == 0) {
                                likeExpression = "%" + search;
                            } else if (i == key.length() - 1) {
                                likeExpression = search + "%";
                            }
                        } else {
                            for (String s: searches) {
                                likeExpression += "%" + s + "%";
                            }
                        }
                        key = key.replace("*", "");
                        List<String> keysconcat = new ArrayList<>(Arrays.asList(searchKeys.split(" ")));
                        if(keysconcat.size() > 1){
                            key = "CONCAT_WS( ' ',"+ String.join(", ", keysconcat)
                                .replace("\"", "") + ")";
                        }
                        String expression = key + " LIKE " + "\'" + likeExpression + "\'";
                        searchParts.add("(" + where + " AND " + expression + ")");
                    });
                    queryWhere = String.join(" OR ", searchParts);
                }
            }

            queryWhere = " WHERE " + queryWhere;

            String queryJoin = this.join(joinsNeeded);

            if (page != null) {
                /*this is the fina query to execute*/
                if(Integer.parseInt(page) < 1){
                    throw new Throwable("page must be higher than 0");
                }

                String queryToExecute = querySelect + queryFrom + queryJoin + queryWhere;
                String queryToCount = "SELECT COUNT("+this.getTableName()+".id) AS items" + queryFrom + queryJoin + queryWhere;
                int numberPage = Integer.valueOf(page);
                int numberLimit = Integer.valueOf(limit);
                if (numberLimit > MAX_LIMIT) {
                    numberLimit = MAX_LIMIT;
                }
                int skip = numberLimit * (numberPage-1);
                toParam = Integer.toString(numberLimit);
                fromParam = Integer.toString(skip);

                // adds order by
                if (orderBy != null) {
                    queryToExecute += " ORDER BY " + this.orderBy(orderBy);
                }
                // adds the limit for pagination
                queryToExecute += " LIMIT " + fromParam + "," + toParam;

                String finalQueryToExecute = queryToExecute;
                dbClient.query(queryToCount, replyCount -> {
                    try {
                        if (replyCount.failed()) {
                            throw replyCount.cause();
                        }
                        dbClient.query(finalQueryToExecute, reply -> {
                            try {
                                if (reply.failed()) {
                                    throw reply.cause();
                                }
                                Integer count = replyCount.result().getRows().get(0).getInteger("items", 0);
                                List<JsonObject> items = reply.result().getRows();

                                JsonObject result = new JsonObject()
                                        .put("count", count)
                                        .put("items", items.size())
                                        .put("results", items);
                                message.reply(result);
                            } catch (Throwable t) {
                                t.printStackTrace();
                                reportQueryError(message, t);
                            }
                        });
                    } catch (Throwable t) {
                        t.printStackTrace();
                        reportQueryError(message, t);
                    }
                });

            } else {
                /*this is the fina query to execute*/
                String queryToExecute = querySelect + queryFrom + queryJoin + queryWhere;
                //adds order by
                if (orderBy != null) {
                    queryToExecute += " ORDER BY " + this.orderBy(orderBy);
                }
                //adds the limit for pagination
                if (fromParam != null && toParam != null) {
                    queryToExecute += " LIMIT " + fromParam + "," + toParam;
                } else if (toParam != null) {
                    queryToExecute += " LIMIT " + toParam;
                }

                dbClient.query(queryToExecute, reply -> {
                    this.genericResponse(message, reply);
                });
            }
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    /**
     * Validates de correct name of the join type
     *
     * @param joinParam the parameter from the URI
     * @return INNER, LEFT or RIGHT
     */
    protected String validateJoinParam(String joinParam) {
        if (joinParam == null) {
            return "INNER";
        }
        if (!(joinParam.equalsIgnoreCase("INNER") || joinParam.equalsIgnoreCase("LEFT") || joinParam.equalsIgnoreCase("RIGHT"))) {
            return "INNER";
        } else {
            return joinParam;
        }
    }

    /**
     * Execute the query "select * from table where id = ?"
     *
     * @param message message from the event bus
     */
    protected void findById(Message<JsonObject> message) {
        try {
            String query = "select * from " + this.getTableName() + " where id = ?";
            JsonArray params = new JsonArray().add(message.body().getInteger("id"));
            dbClient.queryWithParams(query, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    if (reply.result().getNumRows() > 0) {
                        message.reply(reply.result().getRows().get(0));
                    } else {
                        message.reply(null);
                    }
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

    /**
     * Execute the query "delete from table where id = ?"
     *
     * @param message message from the event bus
     */
    protected void deleteById(Message<JsonObject> message) {
        try {
            JsonArray params = new JsonArray().add(message.body().getInteger("id"));
            dbClient.updateWithParams("update " + this.getTableName() + " set status = 3 where id = ?", params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    if (reply.result().getUpdated() == 0) { //does not exist element with id
                        throw new Exception("Element not found");
                    } else {
                        message.reply(null);
                    }
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

    /**
     * Execute the query "create" generated by the properties of the object in the message
     *
     * @param message message from the event bus
     */
    protected void create(Message<JsonObject> message) {
        this.startTransaction(message, con -> {
            try {
                GenericQuery model = this.generateGenericCreate(message.body());
                con.updateWithParams(model.getQuery(), model.getParams(), reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        Integer id = reply.result().getKeys().getInteger(0);
                        this.commit(con, message, new JsonObject().put("id", id));
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(con, t, message);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(con, t, message);
            }
        });
    }

    /**
     * Execute the query "update" generated by the properties of the object in the message
     *
     * @param message message from the event bus
     */
    protected void update(Message<JsonObject> message) {
        try {
            String query = "select status from " + this.getTableName() + " where id = ?";
            JsonArray params = new JsonArray().add(message.body().getInteger("id"));
            dbClient.queryWithParams(query, params, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    if (reply.result().getNumRows() > 0) {
                        int status = reply.result().getRows().get(0).getInteger("status");
                        if (status == 3) {
                            throw new Exception("can't update deleted element");
                        } else {
                            if (status == 0) {
                                if (message.body().getInteger("status") != 1) {
                                    throw new Exception("status can only change to active");
                                } else {
                                    executeUpdate(message);
                                }
                            } else {
                                executeUpdate(message);
                            }
                        }
                    } else {
                        throw new Exception("Element not found");
                    }
                } catch (Throwable t){
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });
            //has to validate that the change of status is permited
            //when a register is in state archived it only be passed to active
            if (message.body().containsKey("status")) {
                //query for the status of the register to update

            } else {
                executeUpdate(message);
            }
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    /**
     * executes the update operation with the properties of the JsonObject
     *
     * @param message message to reply
     */
    protected void executeUpdate(Message<JsonObject> message) {
        try {
            GenericQuery gc = this.generateGenericUpdate(this.getTableName(), message.body());
            dbClient.updateWithParams(gc.getQuery(), gc.getParams(), reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    message.reply(null);
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

    /**
     * Creates the query and params for an update with the provided json object
     *
     * @param tableName name of the table to execute the update
     * @param body json object with the properties to add in the update
     * @return GenericQuery with the string query and the params to update in the table provided
     */
    protected final GenericQuery generateGenericUpdate(String tableName, JsonObject body) {
        String query = "update " + tableName + " set ";
        JsonArray params = new JsonArray();
        for (String fieldName : body.fieldNames()) {
            if (!fieldName.equals("id") && body.getValue(fieldName) != null) {
                query += fieldName + " = ?, ";
                params.add(body.getValue(fieldName));
            }
        }
        query = query.substring(0, query.length() - 2);
        query += " where id = ?";
        params.add(body.getInteger("id"));
        return new GenericQuery(query, params);
    }

    /**
     * Creates the query and params for an update with the provided json object
     *
     * @param tableName name of the table to execute the update
     * @param body json object with the properties to add in the update
     * @return GenericQuery with the string query and the params to update in the table provided
     */
    protected final GenericQuery generateGenericUpdate(String tableName, JsonObject body, boolean persistNull) {
        String query = "update " + tableName + " set ";
        JsonArray params = new JsonArray();
        for (String fieldName : body.fieldNames()) {
            if (!fieldName.equals("id") && (body.getValue(fieldName) != null || body.getValue(fieldName) == null && persistNull)) {
                if (body.getValue(fieldName) != null){
                    query += fieldName + " = ?, ";
                    params.add(body.getValue(fieldName));
                } else {
                    query += fieldName + " = null, ";
                }
            }
        }
        query = query.substring(0, query.length() - 2);
        query += " where id = ?";
        params.add(body.getInteger("id"));
        return new GenericQuery(query, params);
    }

    /**
     * Creates the query and params for an update with the provided json object
     *
     * @param tableName name of the table to execute the update
     * @param body json object with the properties to add in the update
     * @return GenericQuery with the string query and the params to update in the table provided
     */
    protected final GenericQuery generateGenericDelete(String tableName, JsonObject body) {
        StringBuilder query = new StringBuilder("DELETE FROM " + tableName + " WHERE ");
        JsonArray params = new JsonArray();
        int count = 0;
        Set<String> fieldNames = body.fieldNames();
        for (String fieldName : fieldNames) {
            if (body.getValue(fieldName) != null) {
                count++;
                if (fieldNames.size() == count) {
                    query.append(fieldName).append(" = ? ;");
                } else {
                    query.append(fieldName).append(" = ? AND ");
                }
                params.add(body.getValue(fieldName));

            }
        }
        return new GenericQuery(query.toString(), params);
    }

    /**
     * Creates the string query for an update with the provided json object
     *
     * @param tableName name of the table to execute the update
     * @param body json object with the properties to add in the update
     * @deprecated Only we use methods that generate GenericQuery instances.
     * @return GenericQuery with the string query and the params to update in the table provided
     */
    @Deprecated
    public final String generateGenericUpdateString(String tableName, JsonObject body) {
        String query = "update " + tableName + " set ";
        for (String fieldName : body.fieldNames()) {
            if (!fieldName.equals("id") && body.getValue(fieldName) != null) {
                try {
                    query += fieldName + " = '" + body.getString(fieldName) + "', ";
                } catch (Exception e) {
                    query += fieldName + " = " + body.getValue(fieldName) + ", ";
                }
            }
        }
        query = query.substring(0, query.length() - 2);
        query += " where id = " + body.getInteger("id");
        return query;
    }

    /**
     * Execute the count query of all elements in the table of this verticle
     *
     * @param message message from the event bus
     */
    protected final void count(Message<JsonObject> message) {
        try {
            String query = "select count(*) as 'count' from " + this.getTableName();
            dbClient.query(query, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    message.reply(reply.result().getRows().get(0));
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

    /**
     * Execute the query "select {properties} from " where properties are the solicited properties in the query param
     *
     * @param queryParam string query param, that contains the selection and filter to query
     * @return query to excecute in the data base generated with the queryParam
     */
    protected final String select(String queryParam) {
        //clausula de selecccion de campos
        String qSelect = "select ";
        //set de elementos en las clausulas where
        Set<String> qWheres = new LinkedHashSet<>();
        //set de elementos en las clausulas from
        Set<String> qFroms = new LinkedHashSet<>();
        qFroms.add(" from " + this.getTableName());
        //obtener los elementos solicitados
        boolean addStatusFilter = true;
        String[] selections = queryParam.split(","); //fields separation
        for (String selection : selections) {
            if (selection.contains(".") && !isText(selection)) { //a point means that joins with a table
                String[] relation = selection.split("\\.");
                if (relation[0].contains("[")) {
                    relation = selection.split("].");
                    String property = relation[1];
                    String populate = relation[0];
                    populate = populate.substring(1, populate.length());
                    String[] fields = populate.split("=");
                    String localProperty = fields[0];
                    String[] tableFields = fields[1].split("\\.");
                    String table = tableFields[0];
                    String field = tableFields[1];

                    qSelect += table + "." + property + " as " + (table + "_" + property) + ",";
                    qFroms.add(table);
                    qWheres.add(this.getTableName() + "." + localProperty + " = " + table + "." + field);
                } else {
                    WhereSelection ws = new WhereSelection(relation[1]);
                    if (ws.getOperator().isEmpty()) {
                        qSelect += relation[0] + "." + relation[1] + " as " + (relation[0] + "_" + relation[1]) + ",";
                        qFroms.add(relation[0]);
                        qWheres.add(this.getTableName() + "." + relation[0] + "_id = " + relation[0] + ".id");
                    } else {
                        String left = ws.getLeftItem();
                        qSelect += relation[0] + "." + left + " as " + relation[0] + "_" + left + ",";
                        qFroms.add(relation[0]);
                        qWheres.add(this.getTableName() + "." + relation[0] + "_id = " + relation[0] + ".id");
                        qWheres.add(relation[0] + "." + left + " = " + ws.getRightItem());
                    }
                }

            } else {//if no joins then only validate a where clause
                WhereSelection ws = new WhereSelection(selection);
                if (ws.getOperator().isEmpty()) {
                    qSelect += this.getTableName() + "." + selection + ",";
                } else {
                    String left = ws.getLeftItem();
                    if (left.equals("status")) { //ignore de default filter in status column
                        addStatusFilter = false;
                    }
                    qSelect += this.getTableName() + "." + left + ",";
                    qWheres.add(this.getTableName() + "." + ws.getExpresion());
                }
            }
        }
        qSelect = qSelect.substring(0, qSelect.length() - 1);
        String from = String.join(",", qFroms);
        String where = "";
        if (!qWheres.isEmpty()) {
            where += " where ";
            where += String.join(" and ", qWheres);
            if (addStatusFilter) {
                where += " and " + this.getTableName() + ".status != " + Status.DELETED.getValue();
            }
        } else {
            where += " where " + this.getTableName() + ".status != " + Status.DELETED.getValue();
        }
        return qSelect + from + where;
    }

    /**
     * Generates the "SELECT" part of the generic query
     *
     * @param selectParam parameter from the URI
     * @param joinsNeeded Set of join needed for the query
     * @param joinType join type
     * @return return the String with all the selects
     */
    protected final String selectV2(String selectParam, Set<Join> joinsNeeded, String joinType) {
        if (selectParam == null) {
            return "SELECT * ";
        }
        if (selectParam.isEmpty()) {
            return "SELECT * ";
        }
        List<String> fieldsSelect = new ArrayList<>();
        String[] fields = selectParam.split(",");
        for (String field : fields) {
            if (field.contains(".")) {
                String[] parts = field.split("\\.");
                String table = parts[0];
                String tableField = parts[1];
                fieldsSelect.add(table + "." + tableField + " as " + table + "_" + tableField);
                Join join = new Join(joinType, table + "_id", table, table);
                boolean specialJoin = joinsNeeded.stream().anyMatch(j -> j.getAlias().equals(table));
                if (!specialJoin) {
                    joinsNeeded.add(join);
                }
            } else {
                fieldsSelect.add(this.getTableName() + "." + field);
            }
        }
        return "SELECT " + String.join(",", fieldsSelect);
    }

    /**
     * Validates the query param if it is a string
     *
     * @param selection the selecion in the query param
     * @return true if the selection is a specified string
     */
    private boolean isText(final String selection) {
        WhereSelection ws = new WhereSelection(selection);
        String operator = ws.getOperator();
        if (!operator.isEmpty()) {
            String value = ws.rightItem;
            return (value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'"));
        }
        return false;
    }

    /**
     * Generates que query and params needed for a generic create operation
     *
     * @param objectBody the object message to create
     * @return model with the query and needed params to create a register in db of this ServiceDatabaseVerticle
     */
    protected final GenericQuery generateGenericCreate(JsonObject objectBody) {
        String query = "insert into " + getTableName() + "(";
        String queryValues = " values (";
        JsonArray params = new JsonArray();
        for (String fieldName : objectBody.fieldNames()) {
            if (objectBody.getValue(fieldName) != null) {
                query += fieldName + ",";
                queryValues += "?,";
                params.add(objectBody.getValue(fieldName));
            }
        }
        query = query.substring(0, query.length() - 1) + ")";
        queryValues = queryValues.substring(0, queryValues.length() - 1) + ")";
        query += queryValues;
        return new GenericQuery(query, params);
    }
    /**
     * Generates que query and params needed for a generic create operation
     *
     * @param objectBody the object message to create
     * @return model with the query and needed params to create a register in db of this ServiceDatabaseVerticle
     */
    protected final GenericQuery generateGenericCreateSendTableName(String tableName ,JsonObject objectBody) {
        String query = "insert into " + tableName + "(";
        String queryValues = " values (";
        JsonArray params = new JsonArray();
        for (String fieldName : objectBody.fieldNames()) {
            if (objectBody.getValue(fieldName) != null) {
                query += fieldName + ",";
                queryValues += "?,";
                params.add(objectBody.getValue(fieldName));
            }
        }
        query = query.substring(0, query.length() - 1) + ")";
        queryValues = queryValues.substring(0, queryValues.length() - 1) + ")";
        query += queryValues;
        return new GenericQuery(query, params);
    }

    /**
     * Generates a string raw query to insert in database the object in objectBody in the table given
     *
     * @param tableName name of the table to generate the insert
     * @param objectBody object with all the properties to insert
     * @deprecated Only we use methods that generate GenericQuery instances.
     * @return string with the query
     */
    @Deprecated
    public final String generateGenericCreate(final String tableName, final JsonObject objectBody) {
        String query = "insert into " + tableName + "(";
        String queryValues = " values (";
        for (String fieldName : objectBody.fieldNames()) {
            if (objectBody.getValue(fieldName) != null) {
                query += fieldName + ",";
                //evaluate the field if is string
                try {
                    queryValues += "'" + objectBody.getString(fieldName) + "',";
                } catch (Exception e) {
                    queryValues += objectBody.getValue(fieldName) + ",";
                }
            }
        }
        query = query.substring(0, query.length() - 1) + ")";
        queryValues = queryValues.substring(0, queryValues.length() - 1) + ")";
        query += queryValues;
        return query;
    }

    /**
     * Generates a GenericCreate model from a JsonObject in the table specified
     *
     * @param tableName the table name to insert
     * @param objectBody the JsonObeject with the properties to insert
     * @return insert query in string
     */
    protected final GenericQuery generateGenericCreateWithParams(final String tableName, final JsonObject objectBody) {
        String queryValues = "(";
        JsonArray params = new JsonArray();
        for (String fieldName : objectBody.fieldNames()) {
            if (objectBody.getValue(fieldName) != null) {
                queryValues += "?,";
                params.add(objectBody.getValue(fieldName));
            }
        }
        queryValues = queryValues.substring(0, queryValues.length() - 1) + "),";
        return new GenericQuery(queryValues, params);
    }

    /**
     * Generates the insert query from a JsonObject in the table specified
     *
     * @param tableName the table name to insert
     * @param objectBody the JsonObeject with the properties to insert
     * @deprecated Only we use methods that generate GenericQuery instances.
     * @return insert query in string
     */
    @Deprecated
    protected final String generateGenericInsertParams(final String tableName, final JsonObject objectBody) {
        String query = "insert into " + tableName + "(";
        for (String fieldName : objectBody.fieldNames()) {
            if (objectBody.getValue(fieldName) != null) {
                query += fieldName + ",";
            }
        }
        query = query.substring(0, query.length() - 1) + ")";
        query += " values ";
        return query;
    }

    /**
     * Generates the insert query from the jsonArray provided, using all the properties of the objects, considering that the object in the array must have the same number of properties an the equal property name.
     * <b>
     * Example:
     * </b>
     * <p>
     * [{ "name":"test name", "age": 12 },{ "name":"other test name", "age": 14 }...]
     * </p>
     * <p>
     * Should return the next sql code: </p>
     * <b>insert into 'tableName' (name, age) values ('test name', 12),('other test name', 14)</b>
     *
     * @param tableName the name of the table to insert the objects
     * @param jsonArray the object to insert in the table
     * @deprecated Only we use methods that generate GenericQuery instances.
     * @return the sql code in string
     */
    @Deprecated
    protected final String generateGenericInsertWithValues(
            final String tableName,
            final JsonArray jsonArray) {

        StringBuilder sb = new StringBuilder("insert into ") //insert statement
                .append(tableName) //in the table
                .append("("); //start propertie names
        Set<String> propertiesName = new HashSet<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject ob = jsonArray.getJsonObject(i);
            propertiesName.addAll(ob.fieldNames());
        }
        for (String propertieName : propertiesName) {
            sb.append(propertieName).append(",");
        }
        sb.deleteCharAt(sb.length() - 1)//removes the las coma
                //adds the closing ) for properties names and start values                
                .append(") values ");
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject ob = jsonArray.getJsonObject(i);
            sb.append("(");
            for (String fieldName : propertiesName) {
                if (ob.getValue(fieldName) == null) {
                    sb.append("null,");
                    continue;
                }
                try {
                    String value = ob.getString(fieldName);
                    sb.append("'")
                            .append(value)
                            .append("'")
                            .append(",");
                } catch (Exception e) {
                    sb.append(ob.getValue(fieldName)).append(",");
                }
            }
            sb.deleteCharAt(sb.length() - 1)//removes the las coma
                    .append("),");
        }
        return sb.deleteCharAt(sb.length() - 1).append(";").toString();
    }

    /**
     * Need to especifie the name of the entity table, to refer in the properties file, the actions names and queries
     *
     * @return the name of the table to manage in this verticle
     */
    public abstract String getTableName();

    /**
     * Starts a transaction gettin a connection from the pool and setting auto generate keys to true, and auto commit to false
     *
     * @param message message to reply if something goes wrong
     * @param handler the handler called when this operation completes
     */
    protected final void startTransaction(Message<JsonObject> message, Handler<SQLConnection> handler) {
        this.dbClient.getConnection(h -> {
            if (h.succeeded()) {
                SQLConnection con = h.result();
                con.setOptions(new SQLOptions().setAutoGeneratedKeys(true));
                con.setAutoCommit(false, t -> {
                    if (t.succeeded()) {
                        handler.handle(con);
                    } else {
                        con.close();
                        message.fail(ErrorCodes.DB_ERROR.ordinal(), t.cause().getMessage());
                    }
                });
            } else {
                message.fail(ErrorCodes.DB_ERROR.ordinal(), h.cause().getMessage());
            }
        });
    }

    /**
     * Roll back the actual connection in transaction with a generic invalid exception message type to the messager
     *
     * @param con connection in actual transaction
     * @param ex exception with the field and message to display
     * @param message message of the serder
     */
    protected final void rollback(SQLConnection con, UtilsValidation.PropertyValueException ex, Message<JsonObject> message) {
        con.rollback(h -> {
            con.close();
            message.reply(
                    new JsonObject()
                            .put("name", ex.getName())
                            .put("error", ex.getError()),
                    new DeliveryOptions()
                            .addHeader(ErrorCodes.DB_ERROR.toString(),
                                    "invalid parameter")
            );
        });
    }

    /**
     * Roll back the actual connection in transaction with a generic invalid exception message type to the messager
     *
     * @param con connection in actual transaction
     * @param t cause of the fail in an operation in the transaction
     * @param message message of the serder
     */
    protected final void rollback(SQLConnection con, Throwable t, Message<JsonObject> message) {
        con.rollback(h -> {
            con.close();
            reportQueryError(message, t);
        });
    }

    /**
     * Commit the actual transaction and replays to the sender the object provided
     *
     * @param con connection in actual transaction
     * @param message message of the sender
     * @param jsonObject object to reply
     */
    protected final void commit(SQLConnection con, Message<JsonObject> message, JsonObject jsonObject) {
        con.commit(h -> {
            con.close();
            message.reply(jsonObject);
        });
    }

    /**
     * Generic response to avoid boilerplate
     *
     * @param message message to reply
     * @param reply reply from the database query
     */
    protected final void genericResponse(Message<JsonObject> message, AsyncResult<ResultSet> reply) {
        try {
            if (reply.failed()){
                throw reply.cause();
            }
            message.reply(new JsonArray(reply.result().getRows()));
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    /**
     * This method is used to add to the set of joins the specified join in the parameter of the URI
     *
     * @param specialJoinParam parameter from the URI
     * @param joinsNeeded joins needed
     * @param joinParam join type
     */
    protected void addSpecialJoins(String specialJoinParam, Set<Join> joinsNeeded, String joinParam) {
        if (specialJoinParam != null) {
            if (!specialJoinParam.isEmpty()) {
                String[] specialJoins = specialJoinParam.split(",");
                for (String specialJoin : specialJoins) {
                    //[locaField=table.field].alias
                    String[] parts = specialJoin.split("]");
                    String alias = parts[1];
                    String relation = parts[0].substring(1, parts[0].length());
                    String[] relationParts = relation.split("=");
                    String localField = relationParts[0];
                    String table = relationParts[1];
                    //typeJoin JOIN table alias on localField = field
                    Join join = new Join(joinParam, localField, table, alias, true);
                    joinsNeeded.add(join);
                }
            }
        }
    }

    /**
     * Generates the "WHERE" part of the query
     *
     * @param whereParam parameter from the URI
     * @param joinsNeeded joins needed for the query
     * @param joinParam join type
     * @return the String with the WHERE part of the query
     */
    protected final String whereV2(String whereParam, Set<Join> joinsNeeded, final String joinParam) {
        List<String> whereParts = new ArrayList<>();
        if (whereParam != null) {
            if (!whereParam.isEmpty()) {
                String[] wheres = whereParam.split(",");
                for (String where : wheres) {
                    WhereSelection whereSelection = new WhereSelection(where);
                    if (where.contains(".")) {
                        //table.field>20
                        String comparator = whereSelection.getOperator();
                        if (!comparator.isEmpty()) {
                            String[] tablePart = whereSelection.getLeftItem().split("\\.");
                            String table = tablePart[0];
                            if (!table.equals(this.getTableName())) { //other table and needs join
                                //check if the table is a special join
                                boolean exists = joinsNeeded.stream().anyMatch(j -> j.getAlias().equals(table));
                                if (!exists) {
                                    Join join = new Join(joinParam, table + "_id", table, table);
                                    joinsNeeded.add(join);
                                }
                                whereParts.add(whereSelection.getExpresion());
                            } else {
                                whereParts.add(whereSelection.getExpresion());
                            }
                        }
                    } else {
                        //id=10
                        if (!whereSelection.getOperator().isEmpty()) {
                            whereParts.add(whereSelection.getExpresion());
                        }
                    }
                }

            }
        }
        if (whereParts.isEmpty()) {
            return this.getTableName() + ".status != " + Status.DELETED.getValue();
        } else {
            return String.join(" AND ", whereParts);
        }
    }

    /**
     * Generates the "JOIN" part of the query
     *
     * @param joinsNeeded joins needed
     * @return the String with the Join part for the generic query
     */
    protected final String join(Set<Join> joinsNeeded) {
        return String.join(" ", joinsNeeded.stream()
                .map(j -> {
                    if (j.isSpecialJoin()) {
                        return j.getType()
                                + " JOIN "
                                + j.getTable()
                                + " "
                                + j.getAlias()
                                + " ON "
                                + this.getTableName() + "." + j.getMyField()
                                + " = " + j.getAlias() + ".id";
                    } else {
                        return j.getType()
                                + " JOIN "
                                + j.getTable()
                                + " "
                                + j.getAlias()
                                + " ON "
                                + this.getTableName() + "." + j.getMyField()
                                + " = " + j.getTable() + ".id";
                    }
                })
                .collect(toList()));
    }

    protected String orderBy(String orderBy) {
        StringBuilder orderByQuery = new StringBuilder();
        String[] fields = orderBy.split(",");
        for (String field : fields) {
            if (field.contains("|")) {
                field = field.replace('|', ' ');
                orderByQuery.append(field)
                        .append(", ");
            } else {
                orderByQuery.append(field).append(", ");
            }
        }
        String finalQuery = orderByQuery.toString();
        return finalQuery.substring(0, finalQuery.length() - 2);        
    }

    /**
     * Model to represent a join in a query
     */
    protected class Join {

        private String type;
        private String myField;
        private String table;
        private String alias;
        private boolean specialJoin = false;

        public Join(String type, String myField, String table, String alias) {
            this.type = type;
            this.myField = myField;
            this.table = table;
            this.alias = alias;
        }

        public Join(String type, String myField, String table, String alias, boolean specialJoin) {
            this.type = type;
            this.myField = myField;
            this.table = table;
            this.alias = alias;
            this.specialJoin = specialJoin;
        }

        public boolean isSpecialJoin() {
            return specialJoin;
        }

        public void setSpecialJoin(boolean specialJoin) {
            this.specialJoin = specialJoin;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMyField() {
            return myField;
        }

        public void setMyField(String myField) {
            this.myField = myField;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 71 * hash + Objects.hashCode(this.type);
            hash = 71 * hash + Objects.hashCode(this.myField);
            hash = 71 * hash + Objects.hashCode(this.table);
            hash = 71 * hash + Objects.hashCode(this.alias);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Join other = (Join) obj;
            if (!Objects.equals(this.type, other.type)) {
                return false;
            }
            if (!Objects.equals(this.myField, other.myField)) {
                return false;
            }
            if (!Objects.equals(this.table, other.table)) {
                return false;
            }
            if (!Objects.equals(this.alias, other.alias)) {
                return false;
            }
            return true;
        }

    }

    /**
     * Model to represent a join in a query
     */
    protected final class WhereSelection {

        private String selection;
        private String operator;
        private String leftItem;
        private String rightItem;
        private String[] between;
        private String in;

        public String getOperator() {
            return operator;
        }

        public String getLeftItem() {
            return leftItem;
        }

        public String getRightItem() {
            return rightItem;
        }

        WhereSelection(String selection) {
            this.parseSelection(selection);
        }

        private void parseSelection(String selection) {
            this.selection = selection;
            if (selection.contains(">=")) {
                this.operator = ">=";
            } else if (selection.contains("<=")) {
                this.operator = "<=";
            } else if (selection.contains("><")) {
                this.operator = "><";
            } else if (selection.contains("!=")) {
                this.operator = "!=";
            } else if (selection.contains("=~")) {
                this.operator = "=~";
            } else if (selection.contains(">")) {
                this.operator = ">";
            } else if (selection.contains("<")) {
                this.operator = "<";
            } else if (selection.contains("~in~")) {
                this.operator = "~in~";
            } else if (selection.contains("=")) {
                this.operator = "=";
            } else {
                this.operator = "";
            }
            if (!this.operator.isEmpty()) {
                String[] parts = selection.split(Pattern.quote(this.operator));
                this.leftItem = parts[0];
                this.rightItem = parts[1].replace('*', '%');
                if (this.operator == "><") {
                    this.between = parts[1].split("\\|");
                }
                if (this.operator == "~in~") {
                    this.in = rightItem.replace('-', ',');
                }
            }

        }

        private String makeExpresion() {
            return this.leftItem.concat(" ").concat(this.operator).concat(" ").concat(this.rightItem);
        }

        private String makeLikeExpresion() {
            return this.leftItem.concat(" LIKE \'").concat(this.rightItem).concat("\'");
        }

        private String makeBetweenExpresion() {
            return this.leftItem.concat(" BETWEEN ").concat(this.between[0]).concat(" AND ").concat(this.between[1]);
        }

        private String makeInExpresion() {
            return this.leftItem.concat(" IN( ").concat(this.in).concat(" ) ");
        }

        public String getExpresion() {
            if (this.operator.isEmpty()) {
                return "";
            }
            switch (this.operator) {
                case "=~":
                    return this.makeLikeExpresion();
                case "><":
                    return this.makeBetweenExpresion();
                case "~in~":
                    return this.makeInExpresion();
                default:
                    return this.makeExpresion();
            }
        }
    }

    protected void completeQuery(StringBuilder QUERY, JsonArray params, JsonObject body, String propertyName, String QUERY_COMPLEMENT){
        if (body != null && body.getValue(propertyName) != null){
            params.add(body.getValue(propertyName));
        }
        if ((body != null && body.getValue(propertyName) != null && QUERY_COMPLEMENT != null)
            || (params == null && QUERY_COMPLEMENT != null)){
            QUERY.append(QUERY_COMPLEMENT);
        }
    }

    protected void transactionLogger(Message<JsonObject> message) {
        this.startTransaction(message, con -> {
            try {
                GenericQuery model = this.generateGenericCreateSendTableName("logs_transactions", message.body());
                con.updateWithParams(model.getQuery(), model.getParams(), reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        Integer id = reply.result().getKeys().getInteger(0);
                        this.commit(con, message, new JsonObject().put("id", id));
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(con, t, message);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(con, t, message);
            }
        });
    }

    protected void exceptionLogger(Message<JsonObject> message) {
        this.startTransaction(message, con -> {
            try {
                GenericQuery model = this.generateGenericCreateSendTableName("logs_exceptions", message.body());
                con.updateWithParams(model.getQuery(), model.getParams(), reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        Integer id = reply.result().getKeys().getInteger(0);
                        this.commit(con, message, new JsonObject().put("id", id));
                    } catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(con, t, message);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(con, t, message);
            }
        });
    }

    protected CompletableFuture<Integer> exceptionLoggerFuture(Message<JsonObject> message, JsonObject body) {
        CompletableFuture<Integer> future  = new CompletableFuture<>();
        this.startTransaction(message, con -> {
            try {
                GenericQuery model = this.generateGenericCreateSendTableName("logs_exceptions", body);
                con.updateWithParams(model.getQuery(), model.getParams(), reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        Integer id = reply.result().getKeys().getInteger(0);
                        con.commit(h -> {
                            con.close();
                            future.complete(id);
                        });
                    } catch (Throwable t){
                        t.printStackTrace();
                        con.rollback(h -> {
                            con.close();
                            future.completeExceptionally(t);
                        });
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                con.rollback(h -> {
                    con.close();
                    future.completeExceptionally(t);
                });
            }
        });
        return future;
    }

    public CompletableFuture<Boolean> executeUpdate(SQLConnection conn, GenericQuery genericQuery) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            conn.updateWithParams(genericQuery.getQuery(), genericQuery.getParams(), reply -> {
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
        return future;
    }
}
