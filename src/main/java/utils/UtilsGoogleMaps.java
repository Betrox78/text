package utils;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;

import static service.commons.Constants.TERMINAL_ADDRESS;

public class UtilsGoogleMaps {

    private GeoApiContext geoApiContext;

    public UtilsGoogleMaps(GeoApiContext geoApiContext) {
        this.geoApiContext = geoApiContext;
    }

    /**
     * Calculate the percent of the route that remains to be traveled,
     * based on the position of origin, final destination and the current position
     * @param originPosition
     * @param destinyPosition
     * @param currentPosition
     * @return remaining percent
     */
    public int getRemainingPercentOfRoute(String originPosition, String destinyPosition, String currentPosition){
        int percent = 100;
        try {
            String[] origins = {originPosition, currentPosition};
            String[] destinations = {destinyPosition};

            DistanceMatrix matrix = DistanceMatrixApi.getDistanceMatrix(geoApiContext, origins, destinations).await();

            long totalDistance = matrix.rows[0].elements[0].distance.inMeters;
            long currentDistance = matrix.rows[1].elements[0].distance.inMeters;

            percent = (int) ((currentDistance * 100) / totalDistance);

        } catch (ApiException | InterruptedException | IOException e) {
            e.printStackTrace();
        }
        return percent;
    }

    /**
     * Get the distance and travel time of the route
     */
    public JsonArray getDistanceAndTime(String origin, JsonArray destinies) throws IOException, InterruptedException, ApiException {
        DistanceMatrix matrix = DistanceMatrixApi.getDistanceMatrix(geoApiContext, new String[]{origin}, destinies.stream().map(d -> {
            JsonObject destiny = (JsonObject) d;
            return destiny.getString(TERMINAL_ADDRESS);
        }).toArray(String[]::new)).await();

        for(int i = 0; i < matrix.rows[0].elements.length; i ++) {
            DistanceMatrixElement element = matrix.rows[0].elements[i];
            long distance = element.distance.inMeters / 1000;
            long durationSeconds = element.duration.inSeconds;
            long durationHours = durationSeconds / 3600;
            long secondsLeft = durationSeconds % 3600;
            long minutesLeft = secondsLeft / 60;
            String duration = zeroPadLeft(String.valueOf(durationHours)) + ":" + zeroPadLeft(String.valueOf(minutesLeft));
            JsonObject destiny = destinies.getJsonObject(i);
            destiny.put("distance_km", distance).put("travel_time", duration);
        }

        return destinies;
    }

    private String zeroPadLeft(String data) {
        return String.format("%02d", Integer.parseInt(data));
    }

}

