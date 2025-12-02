package service.alliances;

import database.alliances.AllianceCategoryDBV;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;

public class AllianceCategorySV extends ServiceVerticle {
    /**
     * Need to specifie the address of the verticles in the event bus with the access of the db that contains the table
     *
     * @return the name of the registered DBVerticle to work with
     */
    @Override
    protected String getDBAddress() {
        return AllianceCategoryDBV.class.getSimpleName();
    }

    /**
     * Need to especifie the endpoint domain for this verticles begining with "/", ex: return "/example";
     *
     * @return the name to register the verticle in the main router
     */
    @Override
    protected String getEndpointAddress() {
        return "/allianceCategories";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        // Add all generic handlers
        this.addHandler(HttpMethod.GET, "/", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findAll);
        this.addHandler(HttpMethod.GET, "/v2", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findAllV2);
        this.addHandler(HttpMethod.GET, "/:id", AuthMiddleware.getInstance(), this::findById);
        this.addHandler(HttpMethod.GET, "/count", AuthMiddleware.getInstance(), this::count);
        this.addHandler(HttpMethod.GET, "/count/perPage/:num", AuthMiddleware.getInstance(), this::countPerPage);
        this.addHandler(HttpMethod.POST, "/", AuthMiddleware.getInstance(), this::create);
        this.addHandler(HttpMethod.PUT, "/", AuthMiddleware.getInstance(), this::update);
        this.addHandler(HttpMethod.DELETE, "/:id", AuthMiddleware.getInstance(), this::deleteById);
        super.start(startFuture);
    }
}
