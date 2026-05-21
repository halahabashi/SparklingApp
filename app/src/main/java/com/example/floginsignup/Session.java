package com.example.floginsignup;

import java.util.ArrayList;
import java.util.List;

// holds info about the current logged-in user (role, name, how many gate charges)
public class Session {
    public enum Role { ADMIN, CLIENT }

    // only one Session for the whole app
    private static final Session INSTANCE = new Session();
    public static Session get() { return INSTANCE; }

    // how much each gate use costs - TODO maybe move to config later
    public static final double CHARGE_PER_GATE_USD = 10.0;

    private Role role = Role.ADMIN;
    private String userName = "Admin";
    private int clientChargeCount = 0;

    private final List<Listener> listeners = new ArrayList<>();

    public interface Listener { void onSessionChanged(); }

    public Role getRole() { return role; }
    public String getUserName() { return userName; }
    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean isClient() { return role == Role.CLIENT; }

    public void setRole(Role r) {
        role = r;
        // pick name based on role
        if (r == Role.CLIENT) {
            userName = "Client";
        } else {
            userName = "Admin";
        }
        clientChargeCount = 0;
        notifyChanged();
    }

    public int getClientChargeCount() { return clientChargeCount; }
    public double getClientSpend() { return clientChargeCount * CHARGE_PER_GATE_USD; }

    public void incrementClientCharge() {
        clientChargeCount++;
        notifyChanged();
    }

    public void addListener(Listener l) { listeners.add(l); }
    public void removeListener(Listener l) { listeners.remove(l); }

    private void notifyChanged() {
        for (Listener l : new ArrayList<>(listeners)) l.onSessionChanged();
    }
}
