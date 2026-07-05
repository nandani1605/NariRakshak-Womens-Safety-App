package com.example.narirakshak;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SafePlacesActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    Button btnFindPolice, btnFindHospitals;
    LatLng userLocation;

    RecyclerView rvPlaces;
    PlaceAdapter adapter;
    CardView cvPlacesList;

    Button btnClearRoute;
    Polyline currentPolyline;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safe_places);

        btnFindPolice = findViewById(R.id.btnFindPolice);
        btnFindHospitals = findViewById(R.id.btnFindHospitals);
        cvPlacesList = findViewById(R.id.cvPlacesList);
        btnClearRoute = findViewById(R.id.btnClearRoute);

        btnClearRoute.setOnClickListener(v -> {
            if (currentPolyline != null) {
                currentPolyline.remove();
                currentPolyline = null;
            }
            btnClearRoute.setVisibility(View.GONE);

            cvPlacesList.setVisibility(View.VISIBLE);
            cvPlacesList.setTranslationY(800f);
            cvPlacesList.animate().translationY(0f).setDuration(500).start();

            if (userLocation != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 13));
            }
        });

        rvPlaces = findViewById(R.id.rvPlaces);
        rvPlaces.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PlaceAdapter(new ArrayList<>(), place -> {
            cvPlacesList.setVisibility(View.GONE);
            drawRouteInApp(userLocation, place.latLng);
        });
        rvPlaces.setAdapter(adapter);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnFindPolice.setOnClickListener(v -> findNearbyPlaces("police"));
        btnFindHospitals.setOnClickListener(v -> findNearbyPlaces("hospital"));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(false);
        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getDeviceLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void getDeviceLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14));
                }
            });
        } catch (SecurityException e) {
            Log.e("MAP_ERROR", "Permission issue: " + e.getMessage());
        }
    }

    private void findNearbyPlaces(String type) {
        if (userLocation == null) {
            Toast.makeText(this, getString(R.string.map_toast_location), Toast.LENGTH_SHORT).show();
            return;
        }

        mMap.clear();
        cvPlacesList.setVisibility(View.GONE);

        if (currentPolyline != null) {
            currentPolyline.remove();
            currentPolyline = null;
        }
        btnClearRoute.setVisibility(View.GONE);

        // Language ke hisab se text change hoga
        String displayType = type.equals("police") ? getString(R.string.map_btn_police) : getString(R.string.map_btn_hospital);
        Toast.makeText(this, getString(R.string.map_toast_searching, displayType), Toast.LENGTH_SHORT).show();

        String osmType = type.equals("police") ? "police" : "hospital";
        String query = "[out:json];node[\"amenity\"=\"" + osmType + "\"](around:5000," + userLocation.latitude + "," + userLocation.longitude + ");out;";
        String url = "";

        // FIX 1: URL Encode karna zaroori hai nahi toh error aayega
        try {
            url = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray elements = response.getJSONArray("elements");
                        if (elements.length() == 0) {
                            Toast.makeText(this, getString(R.string.map_toast_not_found, displayType), Toast.LENGTH_SHORT).show();
                            adapter.updateList(new ArrayList<>());
                            return;
                        }

                        List<PlaceModel> placeList = new ArrayList<>();

                        for (int i = 0; i < elements.length(); i++) {
                            JSONObject element = elements.getJSONObject(i);
                            double lat = element.getDouble("lat");
                            double lon = element.getDouble("lon");

                            String name = element.has("tags") && element.getJSONObject("tags").has("name")
                                    ? element.getJSONObject("tags").getString("name")
                                    : displayType;

                            LatLng latLng = new LatLng(lat, lon);
                            float markerColor = type.equals("police") ?
                                    BitmapDescriptorFactory.HUE_RED : BitmapDescriptorFactory.HUE_GREEN;

                            mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(name)
                                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

                            float[] resultsDist = new float[1];
                            Location.distanceBetween(userLocation.latitude, userLocation.longitude, lat, lon, resultsDist);
                            double distanceInKm = resultsDist[0] / 1000.0;

                            placeList.add(new PlaceModel(name, distanceInKm, latLng));
                        }

                        Collections.sort(placeList, (p1, p2) -> Double.compare(p1.distance, p2.distance));
                        List<PlaceModel> topFive = placeList.subList(0, Math.min(5, placeList.size()));
                        adapter.updateList(topFive);

                        cvPlacesList.setVisibility(View.VISIBLE);
                        cvPlacesList.setTranslationY(800f);
                        cvPlacesList.animate().translationY(0f).setDuration(500).start();

                    } catch (JSONException e) {
                        Log.e("MAP_ERROR", "JSON Error: " + e.getMessage());
                    }
                },
                error -> {
                    Toast.makeText(this, getString(R.string.map_toast_server_error), Toast.LENGTH_SHORT).show();
                }) {
            // FIX 2: Headers add karna mandatory hai taaki Overpass block na kare
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("User-Agent", "NariRakshakApp/1.0");
                return headers;
            }
        };

        queue.add(request);
    }

    private void drawRouteInApp(LatLng start, LatLng end) {
        Toast.makeText(this, getString(R.string.map_toast_route), Toast.LENGTH_SHORT).show();

        String url = String.format(java.util.Locale.US,
                "https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=polyline",
                start.longitude, start.latitude, end.longitude, end.latitude);

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray routes = response.getJSONArray("routes");
                        if (routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);
                            String geometry = route.getString("geometry");

                            List<LatLng> polylineList = decodePoly(geometry);

                            if (currentPolyline != null) {
                                currentPolyline.remove();
                            }

                            PolylineOptions polylineOptions = new PolylineOptions()
                                    .addAll(polylineList)
                                    .width(12)
                                    .color(android.graphics.Color.parseColor("#311B92"))
                                    .geodesic(true);

                            currentPolyline = mMap.addPolyline(polylineOptions);
                            btnClearRoute.setVisibility(View.VISIBLE);

                            com.google.android.gms.maps.model.LatLngBounds.Builder builder = new com.google.android.gms.maps.model.LatLngBounds.Builder();
                            builder.include(start);
                            builder.include(end);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
                        }
                    } catch (JSONException e) {
                        Log.e("ROUTE_ERROR", "Error parsing route: " + e.getMessage());
                    }
                }, error -> {
            Toast.makeText(this, getString(R.string.map_toast_route_error), Toast.LENGTH_SHORT).show();
        }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("User-Agent", "NariRakshakApp/1.0");
                return headers;
            }
        };
        queue.add(request);
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }
}