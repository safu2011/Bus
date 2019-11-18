package com.example.bus.ModelClasses;

import com.google.android.gms.maps.model.PolylineOptions;


import java.io.Serializable;

public class CustomerModelClass implements Serializable {
    private String name;
    private String id;
    private Double latitude, longitude;
    private Double distanceRemaining;
    private String estimatedArivalTime = null;
    private String deliveryStatus = "Pending";
    private String databaseReference;
    private String phoneNumber;
    private PolylineOptions polylineOptions;
    private Boolean isOnLeave = false;
    public CustomerModelClass(String id, String name, String phoneNumber, String latitude, String longitude, String databaseReference,Boolean isOnLeave) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.latitude = Double.parseDouble(latitude);
        this.longitude = Double.parseDouble(longitude);
        this.databaseReference = databaseReference;
        this.isOnLeave = isOnLeave;
    }

    public String getId() {
        return id;
    }

    public String getCustomerName() {
        return name;
    }

    public String getCustomerPhoneNumber() {
        return phoneNumber;
    }

    public Double getCustomerLatitude() {
        return latitude;
    }

    public Double getCustomerLongitude() {
        return longitude;
    }

    public Double getCustomerDistanceRemaining() {
        return distanceRemaining;
    }

    public String getCustomerEstimatedArivalTime() {
        return estimatedArivalTime;
    }

    public String getCustomerDeliveryStatus() {
        return deliveryStatus;
    }

    public String getCustomerDatabaseReference() {
        return databaseReference;
    }

    public Boolean getIsOnLeave(){ return isOnLeave;}

    public void setCustomerName(String name) {
        this.name = name;
    }

    public void setCustomerPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setCustomerDistanceRemaining(Double distanceRemaining) {
        this.distanceRemaining = distanceRemaining;
    }

    public void setCustomerEstimatedArivalTime(String time) {
        this.estimatedArivalTime = time;
    }

    public void setCustomerDeliveryStatus(String status) {
        this.deliveryStatus = status;
    }

    public void setCustomerPolylineOptions(PolylineOptions polylineOptions){
        this.polylineOptions = polylineOptions;
    }

    public PolylineOptions getCustomerPolyline(){
        return polylineOptions;
    }

    public void setIsOnLeave(Boolean value){
        isOnLeave = value;
    }


}
