package com.example.tourismandroute;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.tourismandroute.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    Button btn;
    String nombre, lugar, Desc;
    int tipo = 1;
    LatLng gps,marker;
    Double LatO, LngO, LatD, LngD;
    List<LatLng> PolilineaLista;
    PolylineOptions PoliConsumida;
    AutocompleteSupportFragment mAutoComplete;
    PlacesClient mPlaces;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_TourismandRoute);
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        btn = findViewById(R.id.btnMap);

        Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));
        mPlaces = Places.createClient(this);
        mAutoComplete = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.places_autocomplete);
        mAutoComplete.setCountry("PE");
        mAutoComplete.setPlaceFields(Arrays.asList(Place.Field.ADDRESS, Place.Field.NAME, Place.Field.LAT_LNG));
        mAutoComplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                nombre = place.getName();
                LatD = place.getLatLng().latitude;
                LngD = place.getLatLng().longitude;
                mMap.clear();
                Marker();
            }
            @Override
            public void onError(@NonNull Status status) {

            }
        });

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if( Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions( this, new String[] {  Manifest.permission.ACCESS_FINE_LOCATION  }, MY_PERMISSION_ACCESS_FINE_LOCATION);
                onMapReady(googleMap);
            } else {
                getMap();
            }
        }else {
            getMap();
        }
    }

    private  void getMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            LatO = location.getLatitude();
            LngO = location.getLongitude();
            gps = new LatLng(LatO, LngO);
            CameraPosition CameraPosition = new CameraPosition.Builder().target(gps).zoom(17).build();
            CameraUpdate update = CameraUpdateFactory.newCameraPosition(CameraPosition);
            mMap.animateCamera(update);
        }
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    private void Marker(){
        marker = new LatLng(LatD, LngD);
        mMap.addMarker(new MarkerOptions().position(marker).title(nombre).snippet(Desc));
        CameraPosition CameraPosition = new CameraPosition.Builder().target(marker).zoom(17).build();
        CameraUpdate update = CameraUpdateFactory.newCameraPosition(CameraPosition);
        mMap.animateCamera(update);
    }

    public void btnTipoClick (View view){
        if (tipo==1){
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            btn.setText("Normal");
            tipo = 0;
        }else{
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            btn.setText("Satelite");
            tipo = 1;
        }
    }

    public void TrazarRuta(View view){

        String url = "https://maps.googleapis.com/maps/api/directions/json?origin="+LatO+","+LngO+"&destination="+LatD+","+LngD+"&key="+getResources().getString(R.string.google_maps_key)+"&mode=drive";

        JsonObjectRequest get = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonArray = response.getJSONArray("routes");
                    JSONObject ruta = jsonArray.getJSONObject(0);
                    JSONObject polilinea = ruta.getJSONObject("overview_polyline");
                    String puntos = polilinea.getString("points");
                    PolilineaLista=decodePoly(puntos);
                    PoliConsumida = new PolylineOptions();
                    PoliConsumida.color(Color.rgb(51, 204, 255));
                    PoliConsumida.width(8f);
                    PoliConsumida.startCap( new SquareCap());
                    PoliConsumida.jointType(JointType.ROUND);
                    PoliConsumida.addAll(PolilineaLista);
                    mMap.addPolyline(PoliConsumida);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        Volley.newRequestQueue(this).add(get);
        CameraPosition CameraPosition = new CameraPosition.Builder().target(midPoint(LatO,LngO,LatD,LngD)).zoom(11).build();
        CameraUpdate update = CameraUpdateFactory.newCameraPosition(CameraPosition);
        mMap.animateCamera(update);
    }

    private LatLng midPoint(double lat1, double long1, double lat2,double long2)
    {
        return new LatLng((lat1+lat2)/2, (long1+long2)/2);
    }

    public static List decodePoly(String encoded) {

        List poly = new ArrayList();
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

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

}