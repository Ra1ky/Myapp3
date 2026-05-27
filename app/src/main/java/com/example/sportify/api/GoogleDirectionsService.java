package com.example.sportify.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GoogleDirectionsService {
    @GET("maps/api/directions/json")
    Call<DirectionsResponse> getDirections(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("waypoints") String waypoints,
            @Query("mode") String mode,
            @Query("key") String apiKey
    );

    class DirectionsResponse {
        @SerializedName("routes")
        public List<Route> routes;
        @SerializedName("status")
        public String status;
    }

    class Route {
        @SerializedName("overview_polyline")
        public OverviewPolyline overviewPolyline;
        @SerializedName("legs")
        public List<Leg> legs;
    }

    class OverviewPolyline {
        @SerializedName("points")
        public String points;
    }

    class Leg {
        @SerializedName("distance")
        public Distance distance;
    }

    class Distance {
        @SerializedName("text")
        public String text;
        @SerializedName("value")
        public int value; // distance in meters
    }
}
