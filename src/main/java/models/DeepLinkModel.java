package models;

import io.vertx.core.json.JsonObject;
import utils.UtilsDeepLinks;

public class DeepLinkModel {
    private final String type;
    private final JsonObject data;
    private final String title;
    private final String description;
    private final String image;
    private final String channel;
    private final String feature;
    private final String campaign;
    private final String stage;
    private final String alias;

    public JsonObject data() {
        JsonObject temp = new JsonObject(data.getMap());
        temp.put("type", type);
        UtilsDeepLinks.putIfNotNull(temp, "$og_title", title);
        UtilsDeepLinks.putIfNotNull(temp, "$og_description", description);
        UtilsDeepLinks.putIfNotNull(temp, "$og_image_url", image);
        return data;
    }
    public String channel() {
        return channel;
    }
    public String feature() {
        return feature;
    }
    public String campaign() {
        return campaign;
    }
    public String stage() {
        return stage;
    }
    public String alias() {
        return alias;
    }

    public static class Builder {
        private final JsonObject data;
        private final String type;
        private String title;
        private String description;
        private String image;
        private String channel;
        private String feature;
        private String campaign;
        private String stage;
        private String alias;

        public Builder(String type, JsonObject data) {
            this.type = type;
            this.data = data;
        }

        public Builder title(String val) {
            title = val;
            return  this;
        }
        public Builder description(String val) {
            description = val;
            return  this;
        }
        public Builder image(String val) {
            image = val;
            return  this;
        }
        public Builder channel(String val) {
            channel = val;
            return  this;
        }
        public Builder feature(String val) {
            feature = val;
            return  this;
        }
        public Builder campaign(String val) {
            campaign = val;
            return  this;
        }
        public Builder stage(String val) {
            stage = val;
            return  this;
        }
        public Builder alias(String val) {
            alias = val;
            return  this;
        }

        public DeepLinkModel build() {
            return new DeepLinkModel(this);
        }
    }

    // Constructor
    private DeepLinkModel(Builder builder) {
        this.type = builder.type;
        this.data = builder.data;
        this.title = builder.title;
        this.description = builder.description;
        this.image = builder.image;
        this.channel = builder.channel;
        this.feature = builder.feature;
        this.campaign = builder.campaign;
        this.stage = builder.stage;
        this.alias = builder.alias;
    }
}