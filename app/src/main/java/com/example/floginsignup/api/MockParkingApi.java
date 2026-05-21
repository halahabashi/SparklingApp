package com.example.floginsignup.api;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

import com.example.floginsignup.Session;
import com.example.floginsignup.model.ActivityItem;
import com.example.floginsignup.model.DashboardData;
import com.example.floginsignup.model.ParkingRow;
import com.example.floginsignup.model.ParkingSpot;
import com.example.floginsignup.model.ParkingState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// fake api - returns hardcoded data with a small delay so it feels like a real network call
public class MockParkingApi implements ParkingApi {

    // pretend network delay
    private static final long FAKE_LATENCY_MS = 250;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<ActivityItem> activityLog = seedActivityLog();
    private long activityIdCounter = activityLog.size();
    private long gateOpenUntilMs = 0L;

    @Override
    public void getDashboard(ApiCallback<DashboardData> cb) {
        handler.postDelayed(() -> cb.onSuccess(buildDashboard()), FAKE_LATENCY_MS);
    }

    @Override
    public void getParkingState(ApiCallback<ParkingState> cb) {
        handler.postDelayed(() -> cb.onSuccess(buildParkingState()), FAKE_LATENCY_MS);
    }

    @Override
    public void getActivityLog(ApiCallback<List<ActivityItem>> cb) {
        handler.postDelayed(() -> cb.onSuccess(new ArrayList<>(activityLog)), FAKE_LATENCY_MS);
    }

    @Override
    public void openGate(GateCallback cb) {
        // open the gate, then auto close it after the duration is up
        handler.postDelayed(() -> {
            gateOpenUntilMs = System.currentTimeMillis() + GATE_OPEN_DURATION_MS;
            recordGateOpened();
            cb.onOpened();
            // schedule the auto-close
            handler.postDelayed(() -> {
                gateOpenUntilMs = 0L;
                cb.onClosed();
            }, GATE_OPEN_DURATION_MS);
        }, FAKE_LATENCY_MS);
    }

    private long gateRemainingMs() {
        return Math.max(0L, gateOpenUntilMs - System.currentTimeMillis());
    }

    private void recordGateOpened() {
        activityIdCounter++;
        Session s = Session.get();
        activityLog.add(0, new ActivityItem(
                String.valueOf(activityIdCounter),
                ActivityItem.Type.GATE_OPENED,
                "Gate opened manually",
                s.getUserName(),
                System.currentTimeMillis()));
        if (s.isClient()) s.incrementClientCharge();
    }

    private DashboardData buildDashboard() {
        List<ActivityItem> recent = new ArrayList<>(activityLog.subList(0, Math.min(3, activityLog.size())));
        return new DashboardData(4, 2, 1, 1, 2840.0, recent);
    }

    private ParkingState buildParkingState() {
        List<ParkingSpot> rowA = Arrays.asList(
                new ParkingSpot("A1", "A", ParkingSpot.Status.OCCUPIED, Color.parseColor("#3B82F6")),
                new ParkingSpot("A2", "A", ParkingSpot.Status.FREE, 0)
        );
        List<ParkingSpot> rowB = Arrays.asList(
                new ParkingSpot("B1", "B", ParkingSpot.Status.OCCUPIED, Color.parseColor("#22C55E")),
                new ParkingSpot("B2", "B", ParkingSpot.Status.FREE, 0)
        );
        List<ParkingRow> rows = Arrays.asList(
                new ParkingRow("ROW A", rowA),
                new ParkingRow("ROW B", rowB)
        );
        long remaining = gateRemainingMs();
        return new ParkingState(remaining > 0, remaining, 2, 1, rows, 2, 1, 1);
    }

    private List<ActivityItem> seedActivityLog() {
        long now = System.currentTimeMillis();
        List<ActivityItem> list = new ArrayList<>();
        list.add(new ActivityItem("1", ActivityItem.Type.CAR_ENTERED,
                "Car entered · Spot A1", "Client", now - minutes(2)));
        list.add(new ActivityItem("2", ActivityItem.Type.CAR_EXITED,
                "Car exited · Spot B1", "Client", now - minutes(15)));
        list.add(new ActivityItem("3", ActivityItem.Type.SUBSCRIPTION,
                "New subscription · Al Hassan", "Admin", now - minutes(10)));
        list.add(new ActivityItem("4", ActivityItem.Type.GATE_OPENED,
                "Gate opened manually", "Admin", now - minutes(32)));
        list.add(new ActivityItem("5", ActivityItem.Type.CAR_ENTERED,
                "Car entered · Spot B1", "Admin", now - hours(1)));
        list.add(new ActivityItem("6", ActivityItem.Type.CAR_EXITED,
                "Car exited · Spot A1", "Admin", now - hours(1)));
        return list;
    }

    private long minutes(int m) { return m * 60_000L; }
    private long hours(int h) { return h * 3_600_000L; }
}
