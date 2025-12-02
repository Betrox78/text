package service.cubic;

import database.cubic.CubicLogDBV;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;

public class CubicLogSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return CubicLogDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/cubicLogs";
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
