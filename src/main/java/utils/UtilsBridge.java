package utils;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.util.concurrent.CompletableFuture;

public class UtilsBridge{

    public WebClient client;
    private String server;
    private int port;

    public UtilsBridge(Vertx vertx, String server, int port){
        this.client = WebClient.create(vertx);
        this.server = server;
        this.port = port;
    }

    public CompletableFuture<JsonObject> post(String uri, JsonObject params){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        client.post(this.port, this.server, uri)
                .putHeader("Authorization", UtilsJWT.getPublicToken())
                .sendJsonObject(params, response -> {
                    if (response.succeeded()){
                        HttpResponse<Buffer> httpResponse = response.result();
                        JsonObject responseInsert = new JsonObject(httpResponse.bodyAsString());
                        if (responseInsert.getString("status").equals("OK")){
                            future.complete(new JsonObject(httpResponse.bodyAsString()));
                        }else{
                            future.completeExceptionally(new Throwable(responseInsert.toString()));
                        }
                    } else {
                        future.completeExceptionally(response.cause());
                    }
                });
        return future;
    }

    public CompletableFuture<JsonObject> post(String uri, JsonObject headers, JsonObject params){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        HttpRequest<Buffer> request = client.post(this.port, this.server, uri);
        headers.fieldNames().forEach(h ->{
            request.putHeader(h, headers.getString(h));
        });
        request.sendJsonObject(params, response -> {
            if (response.succeeded()){
                HttpResponse<Buffer> httpResponse = response.result();
                future.complete(new JsonObject(httpResponse.bodyAsString()));
            } else {
                future.completeExceptionally(response.cause());
            }
        });
        return future;
    }

    public CompletableFuture<JsonObject> update(String uri, JsonObject params){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        client.put(this.port, this.server, uri)
                .putHeader("Authorization", UtilsJWT.getPublicToken())
                .sendJsonObject(params, response -> {
                    if (response.succeeded()){
                        HttpResponse<Buffer> httpResponse = response.result();
                        JsonObject responseInsert = new JsonObject(httpResponse.bodyAsString());
                        if (responseInsert.getString("status").equals("OK")){
                            future.complete(new JsonObject(httpResponse.bodyAsString()));
                        }else{
                            future.completeExceptionally(new Throwable(responseInsert.toString()));
                        }
                    } else {
                        future.completeExceptionally(response.cause());
                    }
                });
        return future;
    }

    public CompletableFuture<JsonObject> update(String uri, JsonObject headers, JsonObject params){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        HttpRequest<Buffer> request = client.put(this.port, this.server, uri);
        headers.fieldNames().forEach(h ->{
            request.putHeader(h, headers.getString(h));
        });
        request.sendJsonObject(params, response -> {
            if (response.succeeded()){
                HttpResponse<Buffer> httpResponse = response.result();
                future.complete(new JsonObject(httpResponse.bodyAsString()));
            } else {
                future.completeExceptionally(response.cause());
            }
        });
        return future;
    }

    public CompletableFuture<JsonObject> get(String uri){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        client.get(this.port, this.server, uri)
                .putHeader("Authorization", UtilsJWT.getPublicToken())
                .send( response -> {
                    if (response.succeeded()){
                        HttpResponse<Buffer> httpResponse = response.result();
                        future.complete(new JsonObject(httpResponse.bodyAsString()));
                    } else {
                        future.completeExceptionally(response.cause());
                    }
                });
        return future;
    }

    public CompletableFuture<JsonObject> get(String uri, JsonObject headers){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        HttpRequest<Buffer> request = client.get(this.port, this.server, uri);
        headers.fieldNames().forEach(h ->{
            request.putHeader(h, headers.getString(h));
        });
        request.send(response -> {
            HttpResponse<Buffer> httpResponse = response.result();
            if (response.succeeded()){
                future.complete(new JsonObject(httpResponse.bodyAsString()));
            } else {
                future.completeExceptionally(response.cause());
            }
        });
        return future;
    }
}