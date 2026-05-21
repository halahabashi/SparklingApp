package com.example.floginsignup.api;

import com.example.floginsignup.model.ActivityItem;
import com.example.floginsignup.model.DashboardData;
import com.example.floginsignup.model.ParkingState;

import java.util.List;

/**
 * Data source contract for the parking app.
 * Swap MockParkingApi for a real implementation (Retrofit, Firebase, gRPC...) without
 * touching UI code by replacing the singleton inside ApiClient.
 */
public interface ParkingApi {
    void getDashboard(ApiCallback<DashboardData> cb);
    void getParkingState(ApiCallback<ParkingState> cb);
    void getActivityLog(ApiCallback<List<ActivityItem>> cb);

    /**
     * Opens the gate for {@link #GATE_OPEN_DURATION_MS}, then auto-closes.
     * onSuccess fires when the gate finishes opening (i.e. is now open);
     * onClosed fires after auto-close.
     */
    void openGate(GateCallback cb);

    long GATE_OPEN_DURATION_MS = 15_000L;

    interface GateCallback {
        void onOpened();
        void onClosed();
        void onError(Throwable error);
    }
}
