package com.example.floginsignup.model;

import java.util.List;

public class ParkingState {
    public final boolean gateOpen;
    public final long gateRemainingMs;
    public final int entryCount;
    public final int exitCount;
    public final List<ParkingRow> rows;
    public final int occupied;
    public final int available;
    public final int reserved;

    public ParkingState(boolean gateOpen, long gateRemainingMs, int entryCount, int exitCount,
                        List<ParkingRow> rows, int occupied, int available, int reserved) {
        this.gateOpen = gateOpen;
        this.gateRemainingMs = gateRemainingMs;
        this.entryCount = entryCount;
        this.exitCount = exitCount;
        this.rows = rows;
        this.occupied = occupied;
        this.available = available;
        this.reserved = reserved;
    }

    public int total() { return occupied + available + reserved; }
    // returns % of spots used, 0-100
    public int occupancyPercent() {
        int t = total();
        // avoid divide by zero
        if (t == 0) return 0;
        return Math.round((occupied * 100f) / t);
    }
}
