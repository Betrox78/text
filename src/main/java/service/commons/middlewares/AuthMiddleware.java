package service.commons.middlewares;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import utils.UtilsJWT;
import utils.UtilsResponse;

import static service.commons.Constants.AUTHORIZATION;
import static service.commons.Constants.USER_ID;
import static service.commons.Constants.TOKEN;

public class AuthMiddleware implements Handler<RoutingContext> {

    private static AuthMiddleware instance = new AuthMiddleware();

    public static AuthMiddleware getInstance() {
        return instance;
    }

    private AuthMiddleware() {
    }

    @Override
    public void handle(RoutingContext context) {
        String token = context.request().headers().get(AUTHORIZATION);
        if (UtilsJWT.isTokenValid(token)) {
            Integer userID = context.get(USER_ID);
            if (userID == null) {
                context.put(USER_ID, UtilsJWT.getUserIdFrom(token))
                        .put(TOKEN, token);
            }
            context.next();
        } else {
            UtilsResponse.responseInvalidToken(context);
        }
    }
}
