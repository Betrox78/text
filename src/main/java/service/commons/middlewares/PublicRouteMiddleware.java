package service.commons.middlewares;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import utils.UtilsJWT;
import utils.UtilsResponse;

import static service.commons.Constants.AUTHORIZATION;
import static service.commons.Constants.USER_ID;

public class PublicRouteMiddleware implements Handler<RoutingContext> {

    private static PublicRouteMiddleware instance = new PublicRouteMiddleware();

    public static PublicRouteMiddleware getInstance() {
        return instance;
    }

    private PublicRouteMiddleware() {
    }

    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        MultiMap headers = request.headers();
        String token = headers.get(AUTHORIZATION);
        if (token == null) {
            headers.add(AUTHORIZATION, UtilsJWT.getPublicToken());
        } else if (!UtilsJWT.isTokenValid(token)) {
            context.put(USER_ID, UtilsJWT.getUserIdFrom(token));
            headers.remove(AUTHORIZATION);
            headers.add(AUTHORIZATION, UtilsJWT.getPublicToken());
        }
        context.next();
    }
}
