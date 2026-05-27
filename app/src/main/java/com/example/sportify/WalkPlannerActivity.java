package com.example.sportify;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sportify.api.GoogleDirectionsService;
import com.example.sportify.db.AppDatabase;
import com.example.sportify.db.DailyRecord;
import com.example.sportify.db.DailyRecordDAO;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.PolyUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WalkPlannerActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "WalkPlannerActivity";
    private GoogleMap mMap;
    private TextView tvDistanceToday, tvPlannedDistance, tvInstructions, tvPlannedLabel;
    private MaterialButton btnConfirm, btnDone, btnClearRoute;
    private FloatingActionButton fabGps;
    
    private final List<LatLng> plannedPoints = new ArrayList<>();
    private final List<Marker> markers = new ArrayList<>();
    private Polyline plannedPolyline;
    private double currentPlannedDistanceKm = 0;
    
    private AppDatabase db;
    private DailyRecordDAO recordDao;
    private String todayDate;
    private DailyRecord todayRecord;

    private boolean isWalkingModeActive = false;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LatLng lastKnownLocation;

    private GoogleDirectionsService directionsService;
    private final String GOOGLE_API_KEY = "AIzaSyDGIY_QmRWG5RVq1GkyMjfGJAyZ18jyMY4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_walk_planner);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.plannerRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        tvDistanceToday = findViewById(R.id.tvDistanceToday);
        tvPlannedDistance = findViewById(R.id.tvPlannedDistance);
        tvInstructions = findViewById(R.id.tvInstructions);
        tvPlannedLabel = findViewById(R.id.tvPlannedLabel);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnDone = findViewById(R.id.btnDone);
        btnClearRoute = findViewById(R.id.btnClearRoute);
        fabGps = findViewById(R.id.fabGps);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        db = SportifyApp.getDatabase();
        recordDao = db.dailyRecordDAO();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize Retrofit for Google Directions API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        directionsService = retrofit.create(GoogleDirectionsService.class);

        loadTodayDistance();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnConfirm.setOnClickListener(v -> enterWalkingMode());
        btnDone.setOnClickListener(v -> showCompletionDialog());
        btnClearRoute.setOnClickListener(v -> showClearConfirmation());
        fabGps.setOnClickListener(v -> moveToCurrentLocation());
        
        animateEntrance();
    }

    private void animateEntrance() {
        View cardStats = findViewById(R.id.cardStats);
        if (cardStats != null) {
            cardStats.setAlpha(0f);
            cardStats.setTranslationY(-50f);
            cardStats.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void loadTodayDistance() {
        new Thread(() -> {
            todayRecord = recordDao.getByDate(todayDate);
            if (todayRecord == null) {
                todayRecord = new DailyRecord(todayDate);
                todayRecord.setStepGoal(10000);
                recordDao.insertOrUpdate(todayRecord);
            }
            
            double km = (todayRecord.getSteps() * 0.75) / 1000.0;
            runOnUiThread(() -> tvDistanceToday.setText(String.format(Locale.getDefault(), "%.1f km", km)));
        }).start();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        
        // Default place: Kaunas
        LatLng kaunas = new LatLng(54.8985, 23.9036); 
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kaunas, 14f));

        startLocationTracking();

        mMap.setOnMapClickListener(latLng -> {
            if (!isWalkingModeActive) {
                addPointToRoute(latLng);
            } else {
                Toast.makeText(this, "Complete route first or clear to plan again", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false); 

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    lastKnownLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    
                    if (plannedPoints.isEmpty()) {
                        addPointToRoute(lastKnownLocation);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastKnownLocation, 16f));
                    }

                    if (isWalkingModeActive) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(lastKnownLocation));
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void moveToCurrentLocation() {
        if (lastKnownLocation != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastKnownLocation, 16f));
        }
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        if (isWalkingModeActive) return true;

        new AlertDialog.Builder(this)
                .setTitle("Remove Waypoint")
                .setMessage("Remove this point from your route?")
                .setPositiveButton("Remove", (dialog, which) -> removePoint(marker))
                .setNegativeButton("Cancel", null)
                .show();
        return true;
    }

    private void addPointToRoute(LatLng point) {
        plannedPoints.add(point);
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(point)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        if (marker != null) markers.add(marker);

        updatePolyline();
    }

    private void removePoint(Marker marker) {
        int index = markers.indexOf(marker);
        if (index != -1) {
            plannedPoints.remove(index);
            markers.remove(index);
            marker.remove();
            updatePolyline();
        }
    }

    private void updatePolyline() {
        if (plannedPolyline != null) plannedPolyline.remove();

        if (plannedPoints.size() > 1) {
            fetchRoadRoute();
        } else {
            tvPlannedDistance.setText("0.0 km");
            btnConfirm.setVisibility(View.GONE);
            btnDone.setVisibility(View.GONE);
            btnClearRoute.setVisibility(plannedPoints.isEmpty() ? View.GONE : View.VISIBLE);
            tvInstructions.setVisibility(View.VISIBLE);
            tvInstructions.setText("Tap map to plan path along roads.");
            currentPlannedDistanceKm = 0;
        }
    }

    private void fetchRoadRoute() {
        if (plannedPoints.size() < 2) return;

        // Draw straight lines as a preview while fetching road geometry
        drawStraightLinesFallback();

        LatLng origin = plannedPoints.get(0);
        LatLng destination = plannedPoints.get(plannedPoints.size() - 1);
        
        StringBuilder waypoints = new StringBuilder();
        if (plannedPoints.size() > 2) {
            for (int i = 1; i < plannedPoints.size() - 1; i++) {
                LatLng p = plannedPoints.get(i);
                waypoints.append(p.latitude).append(",").append(p.longitude);
                if (i < plannedPoints.size() - 2) waypoints.append("|");
            }
        }

        String originStr = origin.latitude + "," + origin.longitude;
        String destStr = destination.latitude + "," + destination.longitude;
        String waypointsStr = waypoints.length() > 0 ? waypoints.toString() : null;

        Log.d(TAG, "Requesting road route: " + originStr + " -> " + destStr);

        directionsService.getDirections(originStr, destStr, waypointsStr, "walking", GOOGLE_API_KEY)
                .enqueue(new Callback<GoogleDirectionsService.DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<GoogleDirectionsService.DirectionsResponse> call, Response<GoogleDirectionsService.DirectionsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String status = response.body().status;
                            if ("OK".equals(status) && response.body().routes != null && !response.body().routes.isEmpty()) {
                                applyRoadPolyline(response.body().routes.get(0));
                            } else {
                                Log.e(TAG, "Directions API Status: " + status);
                                if (!"ZERO_RESULTS".equals(status)) {
                                    Toast.makeText(WalkPlannerActivity.this, "Road snap: " + status, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<GoogleDirectionsService.DirectionsResponse> call, Throwable t) {
                        Log.e(TAG, "Network fail: " + t.getMessage());
                    }
                });
    }

    private void applyRoadPolyline(GoogleDirectionsService.Route route) {
        if (plannedPolyline != null) plannedPolyline.remove();

        List<LatLng> decodedPath = PolyUtil.decode(route.overviewPolyline.points);
        
        PolylineOptions options = new PolylineOptions()
                .addAll(decodedPath)
                .width(14)
                .color(Color.parseColor("#B8FF00")) // sportify_green
                .jointType(JointType.ROUND)
                .startCap(new RoundCap())
                .endCap(new RoundCap());
        
        plannedPolyline = mMap.addPolyline(options);

        // Accurate distance from road path
        long totalMeters = 0;
        for (GoogleDirectionsService.Leg leg : route.legs) {
            if (leg.distance != null) {
                totalMeters += leg.distance.value;
            }
        }
        currentPlannedDistanceKm = totalMeters / 1000.0;
        tvPlannedDistance.setText(String.format(Locale.getDefault(), "%.1f km", currentPlannedDistanceKm));

        btnConfirm.setVisibility(View.VISIBLE);
        btnClearRoute.setVisibility(View.VISIBLE);
        tvInstructions.setVisibility(View.GONE);
    }

    private void drawStraightLinesFallback() {
        if (plannedPolyline != null) plannedPolyline.remove();
        
        PolylineOptions options = new PolylineOptions()
                .addAll(plannedPoints)
                .width(14)
                .color(Color.parseColor("#B8FF00"))
                .jointType(JointType.ROUND)
                .startCap(new RoundCap())
                .endCap(new RoundCap());
        
        plannedPolyline = mMap.addPolyline(options);
        
        double totalMeters = 0;
        for (int i = 0; i < plannedPoints.size() - 1; i++) {
            float[] results = new float[1];
            Location.distanceBetween(
                    plannedPoints.get(i).latitude, plannedPoints.get(i).longitude,
                    plannedPoints.get(i+1).latitude, plannedPoints.get(i+1).longitude,
                    results);
            totalMeters += results[0];
        }
        currentPlannedDistanceKm = totalMeters / 1000.0;
        tvPlannedDistance.setText(String.format(Locale.getDefault(), "%.1f km", currentPlannedDistanceKm));
        
        btnConfirm.setVisibility(View.VISIBLE);
        btnClearRoute.setVisibility(View.VISIBLE);
    }

    private void enterWalkingMode() {
        if (plannedPoints.size() < 2) return;
        isWalkingModeActive = true;
        btnConfirm.setVisibility(View.GONE);
        btnDone.setVisibility(View.VISIBLE);
        
        tvInstructions.setVisibility(View.VISIBLE);
        tvInstructions.setText("Walking Mode Active 🏃");
        tvPlannedLabel.setText("Goal Distance");
    }

    private void showClearConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Route")
                .setMessage("Remove markers and start over?")
                .setPositiveButton("Clear", (dialog, which) -> clearRoute())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearRoute() {
        plannedPoints.clear();
        for (Marker m : markers) m.remove();
        markers.clear();
        if (plannedPolyline != null) plannedPolyline.remove();
        isWalkingModeActive = false;
        currentPlannedDistanceKm = 0;
        tvPlannedDistance.setText("0.0 km");
        tvPlannedLabel.setText("Planned Route");
        btnConfirm.setVisibility(View.GONE);
        btnDone.setVisibility(View.GONE);
        btnClearRoute.setVisibility(View.GONE);
        tvInstructions.setVisibility(View.VISIBLE);
        tvInstructions.setText("Tap map to plan path along roads.");
    }

    private void showCompletionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Complete Walk")
                .setMessage("Add " + String.format(Locale.getDefault(), "%.1f km", currentPlannedDistanceKm) + " to your steps?")
                .setPositiveButton("Complete", (dialog, which) -> completeRoute())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void completeRoute() {
        int additionalSteps = (int) ((currentPlannedDistanceKm * 1000) / 0.75);
        new Thread(() -> {
            todayRecord.setSteps(todayRecord.getSteps() + additionalSteps);
            recordDao.insertOrUpdate(todayRecord);
            runOnUiThread(() -> {
                Toast.makeText(WalkPlannerActivity.this, "Great job! Added " + additionalSteps + " steps.", Toast.LENGTH_LONG).show();
                loadTodayDistance();
                clearRoute();
            });
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) startLocationTracking();
    }
}
