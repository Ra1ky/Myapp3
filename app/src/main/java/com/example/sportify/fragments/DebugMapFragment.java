package com.example.sportify.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sportify.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DebugMapFragment extends Fragment implements OnMapReadyCallback {

    private static final LatLng KAUNAS = new LatLng(54.8985, 23.9036);
    private static final int SEARCH_RADIUS_M = 2000;

    private GoogleMap map;
    private TextView tvStatus;
    // Overpass asks the server for up to 25s; give the client room beyond that
    // so a slow-but-working server isn't cut off at OkHttp's 10s default.
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .build();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_debug_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvStatus = view.findViewById(R.id.tvDebugMapStatus);

        view.findViewById(R.id.btnDebugMapBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        SupportMapFragment mapFrag = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.debugMapFragment);
        if (mapFrag != null) mapFrag.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(KAUNAS, 14));
        tvStatus.setText("Searching for parks in Kaunas…");
        searchNearbyParks(KAUNAS);
    }

    // Public Overpass mirrors. overpass-api.de frequently rate-limits (429) or
    // times out (504) under load, so we fall through to alternates before giving up.
    private static final String[] OVERPASS_ENDPOINTS = {
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
    };

    private void searchNearbyParks(LatLng center) {
        String query = String.format(Locale.US,
                "[out:json][timeout:25];"
                + "(node[\"leisure\"=\"park\"](around:%d,%.6f,%.6f);"
                + "way[\"leisure\"=\"park\"](around:%d,%.6f,%.6f);"
                + "relation[\"leisure\"=\"park\"](around:%d,%.6f,%.6f););"
                + "out center tags;",
                SEARCH_RADIUS_M, center.latitude, center.longitude,
                SEARCH_RADIUS_M, center.latitude, center.longitude,
                SEARCH_RADIUS_M, center.latitude, center.longitude);

        tryOverpassEndpoint(query, 0);
    }

    private void tryOverpassEndpoint(String query, int index) {
        if (index >= OVERPASS_ENDPOINTS.length) {
            mainHandler.post(() -> { if (isAdded()) tvStatus.setText("Parks search failed (all servers busy)"); });
            return;
        }

        Request request = new Request.Builder()
                .url(OVERPASS_ENDPOINTS[index])
                .header("User-Agent", "Sportify-Android/1.0 (park finder)")
                .post(new FormBody.Builder().add("data", query).build())
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                tryOverpassEndpoint(query, index + 1);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    response.close();
                    tryOverpassEndpoint(query, index + 1);
                    return;
                }
                String body = response.body().string();
                mainHandler.post(() -> renderParks(body));
            }
        });
    }

    private void renderParks(String body) {
        if (!isAdded() || map == null) return;
        try {
            JSONArray elements = new JSONObject(body).getJSONArray("elements");
            int count = 0;
            for (int i = 0; i < elements.length(); i++) {
                JSONObject el = elements.getJSONObject(i);
                double lat, lon;
                JSONObject centerObj = el.optJSONObject("center");
                if (centerObj != null) {
                    lat = centerObj.getDouble("lat");
                    lon = centerObj.getDouble("lon");
                } else if (el.has("lat") && el.has("lon")) {
                    lat = el.getDouble("lat");
                    lon = el.getDouble("lon");
                } else {
                    continue;
                }
                JSONObject tags = el.optJSONObject("tags");
                String name = tags != null ? tags.optString("name", "Park") : "Park";
                map.addMarker(new MarkerOptions()
                        .position(new LatLng(lat, lon))
                        .title(name)
                        .icon(BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_GREEN)));
                count++;
            }
            tvStatus.setText(count > 0 ? count + " parks nearby" : "No parks found");
        } catch (Exception e) {
            tvStatus.setText("Could not parse parks");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
    }
}
