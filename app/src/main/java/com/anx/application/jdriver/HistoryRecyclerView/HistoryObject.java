package com.anx.application.jdriver.HistoryRecyclerView;

public class HistoryObject {
    private String rideId, time;

    public HistoryObject(String rideId, String time){
        this.rideId = rideId;
        this.time = time;
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
}
