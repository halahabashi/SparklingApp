package com.example.floginsignup.model;

public class ParkingSpot {
    public enum Status { FREE, OCCUPIED, RESERVED }

    public final String id;
    public final String row;
    public final Status status;
    public final int carColor;

    public ParkingSpot(String id, String row, Status status, int carColor) {
        this.id = id;
        this.row = row;
        this.status = status;
        this.carColor = carColor;
    }
}
