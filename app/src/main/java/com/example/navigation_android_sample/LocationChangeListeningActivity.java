package com.example.navigation_android_sample;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import com.mapbox.mapboxsdk.location.engine.LocationEngine;
import com.mapbox.mapboxsdk.location.engine.LocationEngineCallback;
import com.mapbox.mapboxsdk.location.engine.LocationEngineRequest;
import com.mapbox.mapboxsdk.location.engine.LocationEngineResult;
//import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.mapboxsdk.location.permissions.PermissionsManager;

import com.mapbox.mapboxsdk.location.permissions.PermissionsListener;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.services.android.navigation.v5.location.engine.LocationEngineProvider;

import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Use the Mapbox Core Library to receive updates when the device changes location.
 */
public class LocationChangeListeningActivity extends AppCompatActivity implements
        OnMapReadyCallback, PermissionsListener {

    private static final long DEFAULT_INTERVAL_IN_MILLISECONDS = 5000;
    private static final long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    MapboxMap mapboxMap;
    private MapView mapView;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationChangeListeningActivityLocationCallback callback =
            new LocationChangeListeningActivityLocationCallback(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("Loggg-gg--g-g-g-g-g-g--g-g-g-g-g--g-g-g--g-g1");
        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this);

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_location_change_listening);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

//        mapboxMap.setStyle(Style.TRAFFIC_NIGHT,
//                new Style.OnStyleLoaded() {
//                    @Override public void onStyleLoaded(@NonNull Style style) {
//                        enableLocationComponent(style);
//                    }
//                });
    }

    /**
     * Initialize the Maps SDK's LocationComponent
     */
    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        System.out.println("Loggg-gg--g-g-g-g-g-g--g-g-g-g-g--g-g-g--g-g2");
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

            // Set the LocationComponent activation options
            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(this, loadedMapStyle)
                            .useDefaultLocationEngine(false)
                            .build();

            // Activate with the LocationComponentActivationOptions object
            locationComponent.activateLocationComponent(locationComponentActivationOptions);

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);

            initLocationEngine();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        System.out.println("Loggg-gg--g-g-g-g-g-g--g-g-g-g-g--g-g-g--g-g3");
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.getLastLocation(callback);
        locationEngine.requestLocationUpdates(request, callback, getMainLooper());

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        System.out.println("Loggg-gg--g-g-g-g-g-g--g-g-g-g-g--g-g-g--g-g4");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        System.out.println("Loggg-gg--g-g-g-g-g-g--g-g-g-g-g--g-g-g--g-g5");
        Toast.makeText(this, R.string.user_location_permission_explanation,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        System.out.println("Loggg-gg--g-g-g-g-g-g--g-g-g-g-g--g-g-g--g-g6");
        if (granted) {
            System.out.println("Loggg-gg--g-g-g-g-g-g--g-g-g-g-g--g-g-g--g-g7");
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            System.out.println("Loggg-gg--g-g-g-g-g-g--g-g-g-g-g--g-g-g--g-g8");
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
        }
    }

    private static class LocationChangeListeningActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<LocationChangeListeningActivity> activityWeakReference;

        LocationChangeListeningActivityLocationCallback(LocationChangeListeningActivity activity) {
            System.out.println("Loggg-gg--g-g-g-g-g-g--g-g-g-g-g--g-g-g--g-g9");
            System.out.println(activity);
            this.activityWeakReference = new WeakReference<>(activity);
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        @Override
        public void onSuccess(LocationEngineResult result) {
            System.out.println("Loggg-gg--g-g-g-g-g-g--g-g-g-g-g--g-g-g--g-g10");
            LocationChangeListeningActivity activity = activityWeakReference.get();

            if (activity != null) {
                System.out.println("Loggg-gg--g-g-g-g-g-g--g-g-g-g-g--g-g-g--g-g11");
                Location location = result.getLastLocation();

                System.out.println("Loggg-gg--g-g-g-g-g-g--g-g-g-g-g--g-g-g--g-g12");
                if (location == null) {
                    return;
                }

                // Create a Toast which displays the new location's coordinates
                Toast.makeText(activity, String.format(activity.getString(R.string.new_location),
                                String.valueOf(result.getLastLocation().getLatitude()),
                                String.valueOf(result.getLastLocation().getLongitude())),
                        Toast.LENGTH_SHORT).show();

                System.out.println("Loggg-gg--g-g-g-g-g-g--g-g-g-g-g--g-g-g--g-g13");
                // Pass the new location to the Maps SDK's LocationComponent
                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    System.out.println("Loggg-gg--g-g-g-g-g-g--g-g-g-g-g--g-g-g--g-g14");
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                    System.out.println("Loggg-gg--g-g-g-g-g-g--g-g-g-g-g--g-g-g--g-g15");
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
            LocationChangeListeningActivity activity = activityWeakReference.get();
            System.out.println("Loggg-gg--g-g-g-g-g-g--g-g-g-g-g--g-g-g--g-g16");
            if (activity != null) {
                System.out.println("Loggg-gg--g-g-g-g-g-g--g-g-g-g-g--g-g-g--g-g17");
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Prevent leaks
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates(callback);
        }
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}