package com.example.floginsignup.model;

public class ActivityItem {
    public enum Type { CAR_ENTERED, CAR_EXITED, SUBSCRIPTION, GATE_OPENED }

    public final String id;
    public final Type type;
    public final String title;
    public final String detail;
    public final long timestampMs;

    public ActivityItem(String id, Type type, String title, String detail, long timestampMs) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.detail = detail;
        this.timestampMs = timestampMs;
    }
}
