package database.alliances;

import database.alliances.handlers.AllianceDBV.*;
import database.commons.DBVerticle;
import database.commons.ErrorCodes;
import database.commons.GenericQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;

public class AllianceDBV extends DBVerticle {

    // Actions
    public static final String REGISTER = "AllianceDBV.register";
    public static final String FIND_BY_ID_WITH_DETAILS = "AllianceDBV.findByIdWithDetails";
    public static final String FIND_ALL_FOR_WEB = "AllianceDBV.findAllForWeb";
    public static final String FIND_STATE_LIST_FOR_WEB = "AllianceDBV.findStateListForWeb";
    public static final String FIND_CITY_BY_STATE_LIST_FOR_WEB = "AllianceDBV.findCityByStateListForWeb";

    // Constants
    public static final String CITIES = "cities";
    public static final  String SERVICES = "services";
    public static final  String ALLIANCE_ID = "alliance_id";
    public static final  String CITY_ID = "city_id";
    public static final String STATE_ID = "state_id";
    public static final  String ALLIANCE_CITY = "alliance_city";
    public static final String ALLIANCE_CATEGORY_ID = "alliance_category_id";
    public static final  String ALLIANCE_SERVICE = "alliance_service";
    public static final String PAGE = "page";
    public static final String LIMIT = "limit";
    public static final String ORDER_BY = "orderBy";
    public static final String ID = "id";

    private Register register;
    private FindByIdWithDetails findByIdWithDetails;
    private FindAllForWeb findAllForWeb;
    private StateListForWeb stateListForWeb;
    private CityListByIdForWeb cityListByIdForWeb;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        this.register = new Register(this);
        this.findByIdWithDetails = new FindByIdWithDetails(this);
        this.findAllForWeb = new FindAllForWeb(this);
        this.stateListForWeb = new StateListForWeb(this);
        this.cityListByIdForWeb = new CityListByIdForWeb(this);
    }

    @Override
    public String getTableName() {
        return "alliance";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case REGISTER:
                this.register.handle(message);
                break;
            case FIND_BY_ID_WITH_DETAILS:
                this.findByIdWithDetails.handle(message);
                break;
            case FIND_ALL_FOR_WEB:
                this.findAllForWeb.handle(message);
                break;
            case FIND_STATE_LIST_FOR_WEB:
                this.stateListForWeb.handle(message);
                break;
            case FIND_CITY_BY_STATE_LIST_FOR_WEB:
                this.cityListByIdForWeb.handle(message);
                break;
        }
    }
}
