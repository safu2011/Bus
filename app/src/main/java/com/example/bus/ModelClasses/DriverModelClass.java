package com.example.bus.ModelClasses;


import java.util.ArrayList;

public class DriverModelClass {
    private String id, name, number, vehicleType, childrenInVehicle;
    private String arrivalTime = "3 min";
    private int vehicleCapacity, seatsAvailable, rating , averageRating;
    private ArrayList<String> instituteList;



    public DriverModelClass(String id, String name, String number, ArrayList<String> instituteList, String vehicalType, int vehicalCapacity, int seatsAvailable) {
        this.id = id;
        this.name = name;
        this.number = number;
        this.instituteList = instituteList;
        this.vehicleType = vehicalType;
        this.vehicleCapacity = vehicalCapacity;
        this.seatsAvailable = seatsAvailable;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }

    public ArrayList<String> getDutyAt() {
        return instituteList;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public int getVehicleCapacity(){ return vehicleCapacity;}

    public int getSeatsAvailable(){return seatsAvailable;}

    public void setArrivalTime(String arrivalTime){
        this.arrivalTime = arrivalTime;
    }

    public String getArrivalTime(){return arrivalTime;}

    public void setRating(int rating){
        this.rating = rating;
    }

    public int getRating(){
        return rating;
    }

    public void setAverageRating(int rating){
        this.averageRating = rating;
    }

    public int getAverageRating(){
        return averageRating;
    }

    public void setChildrenInVehicle(String childrenInVehicle){
        this.childrenInVehicle = childrenInVehicle;
    }

    public String getChildrenInVehicle(){
        return childrenInVehicle;
    }

    public void setDutyAt(ArrayList<String> list){
        instituteList = list;
    }


}
