package com.example.narirakshak;

import com.google.android.gms.maps.model.LatLng;

public class PlaceModel {
    String name;
    double distance;
    LatLng latLng; // Directions nikalne ke liye Naya Add kiya

    public PlaceModel(String name, double distance, LatLng latLng) {
        this.name = name;
        this.distance = distance;
        this.latLng = latLng;
    }
}