package com.example.floginsignup.model;

import java.util.List;

public class DashboardData {
    public final int totalSpots;
    public final int occupied;
    public final int available;
    public final int reserved;
    public final double revenue;
    public final List<ActivityItem> recentActivity;

    public DashboardData(int totalSpots, int occupied, int available, int reserved,
                         double revenue, List<ActivityItem> recentActivity) {
        this.totalSpots = totalSpots;
        this.occupied = occupied;
        this.available = available;
        this.reserved = reserved;
        this.revenue = revenue;
        this.recentActivity = recentActivity;
    }
}
