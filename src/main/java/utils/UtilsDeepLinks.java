package utils;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import models.DeepLinkModel;

import javax.management.RuntimeErrorException;
import java.util.concurrent.CompletableFuture;

/**
 * Singleton class for push notifications
 *
 * @author Hector Flores - hmflores95@gmail.com
 */
public class UtilsDeepLinks {
    private static class utilsDeepLinks {
        private static final UtilsDeepLinks INSTANCE = new UtilsDeepLinks();
    }

    private static String globalBranchKey;
    private static WebClient globalWebClient;

    private final String branchKey;
    private final WebClient client;

    private UtilsDeepLinks() {
        this.branchKey = globalBranchKey;
        this.client = globalWebClient;
    }

    public static UtilsDeepLinks getInstance() {
        return utilsDeepLinks.INSTANCE;
    }

    public static void setGlobalBranchKey(String branchKey) {
        UtilsDeepLinks.globalBranchKey = branchKey;
    }
    public static void setGlobalWebClient(WebClient webClient) {
        UtilsDeepLinks.globalWebClient = webClient;
    }

    public CompletableFuture<String> createLink(DeepLinkModel deepLink) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (branchKey == null) {
            future.completeExceptionally(new IllegalArgumentException("branchKey was not set"));
            return future;
        }

        JsonObject postData = new JsonObject()
                .put("branch_key", branchKey);

        putIfNotNull(postData, "channel", deepLink.channel());
        putIfNotNull(postData, "feature", deepLink.feature());
        putIfNotNull(postData, "campaign", deepLink.campaign());
        putIfNotNull(postData, "stage", deepLink.stage());
        putIfNotNull(postData, "data", deepLink.data());

        client.postAbs("https://api2.branch.io/v1/url")
                .sendJsonObject(postData, ar -> {
                    if (ar.succeeded()) {
                        JsonObject responseBody = ar.result().bodyAsJsonObject();
                        String deepLinkUrl = responseBody.getString("url");

                        System.out.println("Received URL: " + deepLinkUrl);

                        future.complete(deepLinkUrl);
                    } else {
                        System.out.println("Something went wrong " + ar.cause().getMessage());
                        future.completeExceptionally(ar.cause());
                    }
                });

        return future;
    }

    public static void putIfNotNull(JsonObject jsonObject, String key, Object value) {
        if (value != null) {
            jsonObject.put(key, value);
        }
    }
}
