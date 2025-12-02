package service.health;

import database.health.HealthDBV;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import main.MainVerticle;
import service.commons.ServiceVerticle;

import java.util.ArrayList;
import java.util.List;

public class HealthSV extends ServiceVerticle {
    List<HttpMethod> methods = new ArrayList<>();

    @Override
    protected String getDBAddress() {
        return HealthSV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/health";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        String deploymentGroup = MainVerticle.deploymentGroup.name().toLowerCase();
        // Add allowed methods
        methods.add(HttpMethod.GET);
        // Add handlers
        this.addHandler(HttpMethod.GET, "/check", this::check);
        this.addHandler(HttpMethod.GET, "/" + deploymentGroup + "/check", this::check);
        super.start(startFuture);
    }

    @Override
    protected List<HttpMethod> getAllowedMethods() {
        return methods;
    }

    private void check(RoutingContext ctx) {
        HttpServerResponse res = ctx.response();
        vertx.eventBus().send(HealthDBV.class.getSimpleName(),
            new JsonObject(),
            options(HealthDBV.HEALTH_CHECK),
            (reply) -> {
                if (reply.failed()) {
                    res.setStatusCode(500).end(reply.cause().getMessage());
                } else {
                    res.setStatusCode(200).end("OK");
                }
        }
        );
    }
}
