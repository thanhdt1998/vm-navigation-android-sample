package com.example.navigation_android_sample;

//import static com.example.navigation_android_sample.NavigationSettings.ACCESS_TOKEN;
//import static com.example.navigation_android_sample.NavigationSettings.STYLE_URL;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.transition.TransitionManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.engine.LocationEngine;
import com.mapbox.mapboxsdk.location.engine.LocationEngineCallback;
import com.mapbox.mapboxsdk.location.engine.LocationEngineRequest;
import com.mapbox.mapboxsdk.location.engine.LocationEngineResult;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener;
import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.ui.v5.route.OnRouteSelectionChangeListener;
import com.mapbox.services.android.navigation.v5.instruction.Instruction;
import com.mapbox.services.android.navigation.v5.location.engine.LocationEngineProvider;
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.milestone.RouteMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Trigger;
import com.mapbox.services.android.navigation.v5.milestone.TriggerProperty;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationTimeFormat;
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.lang.ref.WeakReference;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DualNavigationMapActivity extends AppCompatActivity implements OnNavigationReadyCallback, ProgressChangeListener,
        NavigationListener, Callback<DirectionsResponse>, OnMapReadyCallback, MapboxMap.OnMapClickListener, MapboxMap.OnMapLongClickListener, OnRouteSelectionChangeListener, LocationListener, OffRouteListener {

    private static final int CAMERA_ANIMATION_DURATION = 1000;
    private static final int DEFAULT_CAMERA_ZOOM = 25;
    private ConstraintLayout dualNavigationMap;
    private NavigationView navigationView;
    private MapView mapView;
    private ProgressBar loading;
    private FloatingActionButton launchNavigationFab;
    private Point origin = Point.fromLngLat(106.675789,10.759050);
    private Point currentLocation;
    private Point destination = Point.fromLngLat(106.686777, 10.775056);
    private DirectionsRoute route;
    private boolean isNavigationRunning;
    private MapboxNavigation mapboxNavigation;
    //    private LocationLayerPlugin locationLayer;
    private LocationEngine locationEngine;
    private NavigationMapRoute mapRoute;
    private MapboxMap mapboxMap;
    private Marker currentMarker;
    private boolean locationFound;
    private ConstraintSet navigationMapConstraint;
    private ConstraintSet navigationMapExpandedConstraint;
    private boolean[] constraintChanged;

    private LocationComponent locationComponent;
    private NavigationMapRoute navigationMapRoute;
    private ReplayRouteLocationEngine mockLocationEngine;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationChangeListeningActivityLocationCallback callback =
            new LocationChangeListeningActivityLocationCallback(new LocationChangeListeningActivity());
    private int BEGIN_ROUTE_MILESTONE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this);

        CustomNavigationNotification customNotification = new CustomNavigationNotification(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            customNotification.createNotificationChannel(this);
        }
        MapboxNavigationOptions options = MapboxNavigationOptions.builder()
                .navigationNotification(customNotification)
                .build();

        mapboxNavigation = new MapboxNavigation(this, options);
        mapboxNavigation.addMilestone(
                new RouteMilestone.Builder()
                        .setIdentifier(BEGIN_ROUTE_MILESTONE)
                        .setInstruction(new BeginRouteInstruction())
                        .setTrigger(
                                Trigger.all(
                                        Trigger.lt(TriggerProperty.STEP_INDEX, 3),
                                        Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 200),
                                        Trigger.gte(TriggerProperty.STEP_DISTANCE_TRAVELED_METERS, 75)
                                )
                        ).build()
        );
//        customNotification.register(MyBroadcastReceiver(navigation), context);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setContentView(R.layout.activity_dual_navigation_map);
        initializeViews(savedInstanceState);
         navigationView.initialize(this);
        navigationMapConstraint = new ConstraintSet();
        navigationMapConstraint.clone(dualNavigationMap);
        navigationMapExpandedConstraint = new ConstraintSet();
        navigationMapExpandedConstraint.clone(this, R.layout.activity_dual_navigation_expand_map);

        constraintChanged = new boolean[]{false};
        launchNavigationFab.setOnClickListener(v -> {
            expandCollapse();
            launchNavigation();
        });
    }

    @SuppressLint("MissingInflatedId")
    private void initializeViews(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_dual_navigation_map);
        dualNavigationMap = findViewById(R.id.dualNavigationMap);
        mapView = findViewById(R.id.mapView);
        navigationView = findViewById(R.id.navigationView);
        loading = findViewById(R.id.loading);
        launchNavigationFab = findViewById(R.id.launchNavigation);
        navigationView.onCreate(savedInstanceState);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    private void expandCollapse() {
        TransitionManager.beginDelayedTransition(dualNavigationMap);
        ConstraintSet constraint;
        if (constraintChanged[0]) {
            constraint = navigationMapConstraint;
        } else {
            constraint = navigationMapExpandedConstraint;
        }
        constraint.applyTo(dualNavigationMap);
        constraintChanged[0] = !constraintChanged[0];
    }

    private void launchNavigation() {
        launchNavigationFab.hide();
        navigationView.setVisibility(View.VISIBLE);
        NavigationMapboxMap navigationMapboxMap = navigationView.retrieveNavigationMapboxMap();
        RouteListener routeListener = new RouteListener() {
            @Override
            public boolean allowRerouteFrom(Point point) {
                System.out.println("||||||||||||||||------------------------------allowRerouteFrom");
                return false;
            }

            @Override
            public void onOffRoute(Point point) {
                System.out.println("||||||||||||||||------------------------------onOffRoute");
            }

            @Override
            public void onRerouteAlong(DirectionsRoute directionsRoute) {

                System.out.println("||||||||||||||||------------------------------onRouteAlong");
            }

            @Override
            public void onFailedReroute(String s) {

                System.out.println("||||||||||||||||------------------------------onFailedReRoute");
            }

            @Override
            public void onArrival() {

                System.out.println("||||||||||||||||------------------------------onArrival");
            }
        };
        mapboxNavigation.addOffRouteListener(this);
        NavigationViewOptions.Builder options = NavigationViewOptions.builder()
                .navigationListener(this)
                .routeListener(routeListener)
                .locationEngine(locationEngine)

                .shouldSimulateRoute(false)
                .progressChangeListener(progressChangeListener)
//                .routeListener(offRouteListener)
                .milestoneEventListener(milestoneEventListener)
                .directionsRoute(route);

        NavigationViewOptions.Builder mockOptions = NavigationViewOptions.builder()
                .navigationListener(this)
//                .routeListener(routeListener)
                .locationEngine(mockLocationEngine)



                .progressChangeListener(progressChangeListener)
//                .routeListener(offRouteListener)
                .milestoneEventListener(milestoneEventListener)
                .directionsRoute(route);

        MapboxNavigationOptions options2 = MapboxNavigationOptions.builder()
//                .accessToken(MAPBOX_ACCESS_TOKEN)
                .build();
        MapboxNavigation mapboxNavigation = new MapboxNavigation(this);
        mapboxNavigation.addMilestone(new RouteMilestone.Builder()
                .setIdentifier(1001)
                .setInstruction(new BeginRouteInstruction())
                .setTrigger(
                        Trigger.all(
                                Trigger.lt(TriggerProperty.STEP_INDEX, 3),
                                Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 200),
                                Trigger.gte(TriggerProperty.STEP_DISTANCE_TRAVELED_METERS, 75)
                        )
                ).build());
//        ((ReplayRouteLocationEngine) locationEngine).assign(route);

//        locationEngine.getLastLocation();
        mockLocationEngine.assign(route);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
//        locationEngine.getLastLocation(callback);

        mapboxNavigation.startNavigation(route);
        navigationView.startNavigation(options.build());
    }
    private NavigationEventListener navigationEventListener= b -> {

    };
    private ProgressChangeListener progressChangeListener = (location, routeProgress) -> {
        System.out.println("Progress Changing------------------------");
    };
private  MilestoneEventListener milestoneEventListener= (routeProgress, s, milestone) -> {
};
private  OffRouteListener offRouteListener = location -> {

};
    @Override
    public void onLocationChanged(@NonNull Location location) {
        // Handle new location updates here
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        System.out.println(location);
        System.out.println("=====================================");
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    @Override
    public void userOffRoute(Location location) {
        System.out.println("User Off Route----------------------------------");
        System.out.println(location);
    }

    private static class BeginRouteInstruction extends Instruction {

        @Override
        public String buildInstruction(RouteProgress routeProgress) {
            return "Have a safe trip!";
        }
    }

    private boolean validRouteResponse(Response<DirectionsResponse> response) {
        return response.body() != null && !response.body().routes().isEmpty();
    }


    private void updateLoadingTo(boolean isVisible) {
        if (isVisible) {
            loading.setVisibility(View.VISIBLE);
        } else {
            loading.setVisibility(View.INVISIBLE);
        }
    }

    private void fetchRoute(Point origin, Point destination) {
        NavigationRoute builder = NavigationRoute.builder(this)
                .accessToken("pk.eyJ1Ijoic2VubmQiLCJhIjoiY2tkcmdzbDB4MDhzcDJ6bzdoOXNwazduMSJ9.in-0A1ZX6yzIMtnJcLdcRw")
                .origin(origin)
                .destination(destination)
                .alternatives(true)
                .build();
        builder.getRoute(this);
    }

    private void initMapRoute() {

        mapRoute = new NavigationMapRoute(mapView, mapboxMap);
        mapRoute.setOnRouteSelectionChangeListener(this);
        mapRoute.addProgressChangeListener(new MapboxNavigation(this));
    }

    //    private void initLocationLayer() {
//        locationLayer = new LocationLayerPlugin(mapView, mapboxMap, locationEngine);
//        locationLayer.setRenderMode(RenderMode.COMPASS);
//    }
    private  void  initListenGPS(){
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> getCurrentLocation(), 5000);
    }
    private void getCurrentLocation() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            System.out.println("----------------------------------------------------");
                            System.out.println(location.toString());
                            origin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
                            currentLocation = Point.fromLngLat(location.getLongitude(), location.getLatitude());
                            // Logic to handle location object
                        }
                    });
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        }
    private void initLocationEngine() {
        mockLocationEngine = new ReplayRouteLocationEngine();
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);
//        locationEngine = LocationEngineProvider.getBestLocationEngine(this);
        //// sets the maximum wait time in milliseconds for location updates.
        // Locations determined at intervals but delivered in batch based on wait time.
        // Batching is not supported by all engines.
        long DEFAULT_INTERVAL_IN_MILLISECONDS = 5000;
        long DEFAULT_MAX_WAIT_TIME = 30000;
        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
                .build();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
locationEngine.getLastLocation(callback);
            System.out.println("Loggg--f-f-f-f-f-f-f-f-f-f-f-f-f-f-f-f--f");
            mockLocationEngine.assignLastLocation(origin);
//            locationEngine.getLastLocation(callback);
            locationEngine.requestLocationUpdates(request, callback, Looper.getMainLooper());

//            mockLocationEngine.requestLocationUpdates(request, callback, Looper.getMainLooper());
            return;
        }
//        if (locationEngine.getLastLocation(callback) != null) {
//        if (locationEngine.getLastLocation(callback) != null) {
//            Location lastLocation = locationEngine.getLastLocation();
//            onLocationChanged(lastLocation);
//            origin = Point.fromLngLat(lastLocation.getLongitude(), lastLocation.getLatitude());
//        }
        System.out.println("");
    }

    private void setCurrentMarkerPosition(LatLng position) {
        if (position != null) {
            if (currentMarker == null) {
//                MarkerViewOptions markerViewOptions = new MarkerViewOptions()
//                        .position(position);
//                currentMarker = mapboxMap.addMarker(markerViewOptions);
            } else {
                currentMarker.setPosition(position);
            }
        }
    }

    private static class LocationChangeListeningActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<LocationChangeListeningActivity> activityWeakReference;

        LocationChangeListeningActivityLocationCallback(LocationChangeListeningActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        @Override
        public void onSuccess(LocationEngineResult result) {
            LocationChangeListeningActivity activity = activityWeakReference.get();
           List<Location> locations = result.getLocations();
            System.out.println("---------------------------------");
            System.out.println(locations);
            System.out.println("---------------------------------");
            if (activity != null) {
                Location location = result.getLastLocation();
                System.out.println(location);
                if (location == null) {
                    return;
                }

// Create a Toast which displays the new location's coordinates
//                Toast.makeText(activity, String.format(activity.getString(R.string.new_location),
//                                String.valueOf(result.getLastLocation().getLatitude()),
//                                String.valueOf(result.getLastLocation().getLongitude())),
//                        Toast.LENGTH_SHORT).show();

// Pass the new location to the Maps SDK's LocationComponent
                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                }
            }
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location can't be captured
         *
         * @param exception the exception message
         */
        @Override
        public void onFailure(@NonNull Exception exception) {
            System.out.println("GetLocationFailure---------------------Logg");
            LocationChangeListeningActivity activity = activityWeakReference.get();
            if (activity != null) {
//                Toast.makeText(activity, exception.getLocalizedMessage(),
//                        Toast.LENGTH_SHORT).show();
                System.out.println("Error when update location");
                System.out.println(exception.toString());
            }
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        navigationView.onStart();
        mapView.onStart();
//        if (locationLayer != null) {
//            locationLayer.onStart();
//        }
    }

    @Override
    public void onResume() {
        super.onResume();
        navigationView.onResume();
        mapView.onResume();
        if (locationEngine != null) {
//            locationEngine.addLocationEngineListener(this);
//            if (!locationEngine.isConnected()) {
//                locationEngine.activate();
//            }
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        navigationView.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onBackPressed() {
        if (!navigationView.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        navigationView.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        navigationView.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        navigationView.onPause();
        mapView.onPause();
        if (locationEngine != null) {
//            locationEngine.removeLocationEngineListener(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        navigationView.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        navigationView.onDestroy();
        mapView.onDestroy();
        if (locationEngine != null) {
//            locationEngine.removeLocationUpdates(this);
//            locationEngine.deactivate();
        }
    }

    @Override
    public boolean onMapLongClick(@NonNull LatLng point) {
        destination = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        updateLoadingTo(true);
        setCurrentMarkerPosition(point);
        if (origin != null) {
            fetchRoute(origin, destination);
        }
        return true;
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(new Style.Builder().fromUri("https://api.maptiler.com/maps/streets/style.json?key=AVXR2vOTw3aGpqw8nlv2"), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                initLocationEngine();
                initListenGPS();
                enableLocationComponent(style);
                initMapRoute();
                fetchRoute(origin, destination);
            }
        });
        this.mapboxMap.addOnMapClickListener(this);
    }

    @SuppressLint("MissingPermission")
    private void enableLocationComponent(Style style) {
        // Get an instance of the component
        locationComponent = mapboxMap.getLocationComponent();
        System.out.println("enableLocationComponent============================================================");
        if (locationComponent != null) {
            // Activate with a built LocationComponentActivationOptions object
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, style).build()
            );
            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);
            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING_GPS_NORTH);
            locationComponent.zoomWhileTracking(DEFAULT_CAMERA_ZOOM);
            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.GPS);
            locationComponent.setLocationEngine(locationEngine);
        }
    }

    @Override
    public void onNavigationReady(boolean isRunning) {
        isNavigationRunning = isRunning;
    }

    @Override
    public void onCancelNavigation() {
        navigationView.stopNavigation();
        expandCollapse();
    }

    @Override
    public void onNavigationFinished() {

    }

    @Override
    public void onNavigationRunning() {

    }

    @Override
    public void onNewPrimaryRouteSelected(DirectionsRoute directionsRoute) {
        route = directionsRoute;
    }

    @Override
    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
        if (validRouteResponse(response)) {
            updateLoadingTo(false);
            launchNavigationFab.show();
            route = response.body().routes().get(0);
            mapRoute.addRoutes(response.body().routes());
            if (isNavigationRunning) {
                launchNavigation();
            }
        }
    }

    @Override
    public void onFailure(Call<DirectionsResponse> call, Throwable t) {

    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        origin = currentLocation;
        destination = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        updateLoadingTo(true);
        setCurrentMarkerPosition(point);
        if (origin != null) {
            fetchRoute(origin, destination);
        }
        return false;
    }

    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
        origin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
        fetchRoute(origin, destination);
    }
}