package com.anx.application.jdriver.HistoryRecyclerView;

public class HistoryObject {

    private String rideId, time, rideDestination, rideCost;

    public HistoryObject(String rideId, String time, String rideDestination, String rideCost){
        this.rideId = rideId;
        this.time = time;
        this.rideDestination = rideDestination;
        this.rideCost = rideCost;
    }

    public String getRideId(){
        return rideId;
    }
    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getTime(){
        return time;
    }
    public void setTime(){
        this.time = time;
    }

    public String getRideDestination(){
        return rideDestination;
    }
    public void setRideDestination(){
        this.rideDestination = rideDestination;
    }

    public String getRideCost(){
        return rideCost;
    }
    public void setRideCost(){
        this.rideCost = rideCost;
    }
}
