package com.example.floginsignup.model;

import java.util.List;

public class ParkingRow {
    public final String label;
    public final List<ParkingSpot> spots;

    public ParkingRow(String label, List<ParkingSpot> spots) {
        this.label = label;
        this.spots = spots;
    }
}
