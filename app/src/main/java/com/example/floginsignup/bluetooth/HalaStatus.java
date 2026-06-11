package com.example.floginsignup.bluetooth;

/**
 * Parsed ESP32 status line: "HALA|S:3/4|G:1|L:D|P:0100"
 * S = available/total spots, G = gate open (1/0), L = lighting (N/D),
 * P = per-spot occupancy, one char per spot ('1' = occupied).
 * P is optional (older firmware); spotOccupied is null when absent.
 */
public class HalaStatus {

    public final int availableSpots;
    public final int totalSpots;
    public final boolean gateOpen;
    public final boolean night;
    public final boolean[] spotOccupied;

    private HalaStatus(int availableSpots, int totalSpots, boolean gateOpen,
                       boolean night, boolean[] spotOccupied) {
        this.availableSpots = availableSpots;
        this.totalSpots = totalSpots;
        this.gateOpen = gateOpen;
        this.night = night;
        this.spotOccupied = spotOccupied;
    }

    public int occupiedSpots() {
        return totalSpots - availableSpots;
    }

    /** Returns null if the message is not a HALA status line. */
    public static HalaStatus parse(String msg) {
        if (msg == null || !msg.startsWith("HALA|")) return null;
        try {
            int available = -1, total = -1;
            boolean gateOpen = false, night = false;
            boolean[] spots = null;

            for (String part : msg.split("\\|")) {
                if (part.startsWith("S:")) {
                    String[] frac = part.substring(2).split("/");
                    available = Integer.parseInt(frac[0].trim());
                    total = Integer.parseInt(frac[1].trim());
                } else if (part.startsWith("G:")) {
                    gateOpen = part.substring(2).trim().equals("1");
                } else if (part.startsWith("L:")) {
                    night = part.substring(2).trim().equalsIgnoreCase("N");
                } else if (part.startsWith("P:")) {
                    String bits = part.substring(2).trim();
                    spots = new boolean[bits.length()];
                    for (int i = 0; i < bits.length(); i++) {
                        spots[i] = bits.charAt(i) == '1';
                    }
                }
            }
            if (available < 0 || total <= 0) return null;
            return new HalaStatus(available, total, gateOpen, night, spots);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
