package com.example.bus.ModelClasses;


public class DriverModelClass {
    private String id, name, number, dutyAt, vehicleType;
    private String arrivalTime = "3 min";
    private int vehicleCapacity, seatsAvailable;


    public DriverModelClass(String id, String name, String number, String dutyAt, String vehicalType, int vehicalCapacity, int seatsAvailable) {
        this.id = id;
        this.name = name;
        this.number = number;
        this.dutyAt = dutyAt;
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

    public String getDutyAt() {
        return dutyAt;
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



}
