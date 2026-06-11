package com.example.floginsignup.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.floginsignup.R;
import com.example.floginsignup.api.ApiCallback;
import com.example.floginsignup.api.ApiClient;
import com.example.floginsignup.api.ParkingApi;
import com.example.floginsignup.bluetooth.BluetoothGateManager;
import com.example.floginsignup.bluetooth.HalaStatus;
import com.example.floginsignup.bluetooth.SelectDeviceActivity;

import java.util.Arrays;
import com.example.floginsignup.model.ParkingRow;
import com.example.floginsignup.model.ParkingSpot;
import com.example.floginsignup.model.ParkingState;
import com.example.floginsignup.ui.widget.DonutChartView;

public class ParkingFragment extends Fragment {

    private TextView tvEntry, tvExit, tvOccCount, tvAvlCount, tvOccPctSmall;
    private TextView tvOccPercent, tvDonutOccupied, tvDonutAvailable, tvDonutReserved;
    private LinearLayout rowAContainer, rowBContainer;
    private DonutChartView donut;

    private LinearLayout gateChipContainer;
    private ImageView ivGateChipIcon;
    private TextView tvGateChip, tvGateStatus;

    private LinearLayout btnGate;
    private TextView tvBtnGate;
    private final Handler gateHandler = new Handler(Looper.getMainLooper());
    private Runnable gateTick;
    private boolean gateBusy = false;

    private final BluetoothGateManager gateBt = BluetoothGateManager.getInstance();

    private final BluetoothGateManager.Listener btListener = new BluetoothGateManager.Listener() {
        @Override
        public void onConnected(String deviceName) {
            if (!isAdded()) return;
            toast(getString(R.string.bt_connected, deviceName));
            // Device picked from the gate button flow — send the command now
            sendOpenGate();
        }

        @Override
        public void onConnectionFailed(String reason) {
            if (!isAdded()) return;
            toast(getString(R.string.bt_connection_failed, reason));
            resetGateButton();
        }

        @Override
        public void onDisconnected() {
            if (!isAdded()) return;
            toast(getString(R.string.bt_disconnected));
        }

        @Override
        public void onMessage(String message) {
            if (!isAdded()) return;
            // ESP32 confirms the real gate state — mirror it in the UI
            if (message.equalsIgnoreCase("GATE:OPEN")) {
                applyGateState(true);
            } else if (message.equalsIgnoreCase("GATE:CLOSED")) {
                applyGateState(false);
                // ESP32 closed the gate on its own — stop the countdown
                if (gateBusy) resetGateButton();
            }
        }

        @Override
        public void onStatus(HalaStatus status) {
            if (!isAdded()) return;
            bindLive(status);
        }
    };

    private final ActivityResultLauncher<String> btPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    onGateClicked();
                } else {
                    toast(getString(R.string.bt_permission_needed));
                }
            });

    private final ActivityResultLauncher<Intent> devicePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) {
                    return;
                }
                String name = result.getData().getStringExtra(SelectDeviceActivity.EXTRA_DEVICE_NAME);
                String address = result.getData().getStringExtra(SelectDeviceActivity.EXTRA_DEVICE_ADDRESS);
                if (address == null) return;
                toast(getString(R.string.bt_connecting, name != null ? name : address));
                gateBt.connect(name, address);
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        tvEntry = v.findViewById(R.id.tvEntry);
        tvExit = v.findViewById(R.id.tvExit);
        tvOccCount = v.findViewById(R.id.tvOccCount);
        tvAvlCount = v.findViewById(R.id.tvAvlCount);
        tvOccPctSmall = v.findViewById(R.id.tvOccPctSmall);
        tvOccPercent = v.findViewById(R.id.tvOccPercent);
        tvDonutOccupied = v.findViewById(R.id.tvDonutOccupied);
        tvDonutAvailable = v.findViewById(R.id.tvDonutAvailable);
        tvDonutReserved = v.findViewById(R.id.tvDonutReserved);
        rowAContainer = v.findViewById(R.id.rowAContainer);
        rowBContainer = v.findViewById(R.id.rowBContainer);
        donut = v.findViewById(R.id.donut);

        gateChipContainer = v.findViewById(R.id.gateChipContainer);
        ivGateChipIcon = v.findViewById(R.id.ivGateChipIcon);
        tvGateChip = v.findViewById(R.id.tvGateChip);
        tvGateStatus = v.findViewById(R.id.tvGateStatus);

        btnGate = v.findViewById(R.id.btnGate);
        tvBtnGate = v.findViewById(R.id.tvBtnGate);
        btnGate.setOnClickListener(view -> onGateClicked());

        gateBt.addListener(btListener);

        ApiClient.get().getParkingState(new ApiCallback<ParkingState>() {
            @Override public void onSuccess(ParkingState data) { bind(data); }
            @Override public void onError(Throwable e) { /* show empty */ }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        gateHandler.removeCallbacksAndMessages(null);
        gateBt.removeListener(btListener);
    }

    private void onGateClicked() {
        // already opening, don't do it twice
        if (gateBusy) return;

        // Need the Bluetooth runtime permission before anything else
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
            return;
        }

        // Not connected to the gate controller yet — pick a paired device first;
        // the command is sent from the onConnected callback
        if (!gateBt.isConnected()) {
            devicePickerLauncher.launch(new Intent(requireContext(), SelectDeviceActivity.class));
            return;
        }

        sendOpenGate();
    }

    // Sends "O" to the ESP32 over Bluetooth and runs the usual open/countdown flow
    private void sendOpenGate() {
        if (gateBusy) return;
        gateBusy = true;
        btnGate.setEnabled(false);

        gateBt.openGate();

        ApiClient.get().openGate(new ParkingApi.GateCallback() {
            @Override
            public void onOpened() {
                if (!isAdded()) return;
                applyGateState(true);
                startGateCountdown(ParkingApi.GATE_OPEN_DURATION_MS);
            }

            @Override
            public void onClosed() {
                if (!isAdded()) return;
                if (!gateBusy) return;
                gateBt.closeGate();
                applyGateState(false);
                resetGateButton();
            }

            @Override
            public void onError(Throwable error) {
                if (!isAdded()) return;
                resetGateButton();
            }
        });
    }

    private void applyGateState(boolean open) {
        Context ctx = requireContext();
        if (open) {
            gateChipContainer.setBackgroundResource(R.drawable.bg_chip_green);
            int green = ContextCompat.getColor(ctx, R.color.brand_green);
            ivGateChipIcon.setColorFilter(green, PorterDuff.Mode.SRC_IN);
            tvGateChip.setText(R.string.gate_open);
            tvGateChip.setTextColor(green);
            tvGateStatus.setText(R.string.gate_status_open);
        } else {
            gateChipContainer.setBackgroundResource(R.drawable.bg_chip_red);
            int red = ContextCompat.getColor(ctx, R.color.brand_red);
            ivGateChipIcon.setColorFilter(red, PorterDuff.Mode.SRC_IN);
            tvGateChip.setText(R.string.gate_closed_chip);
            tvGateChip.setTextColor(red);
            tvGateStatus.setText(R.string.gate_closed);
        }
    }

    private void startGateCountdown(long remainingMs) {
        gateBusy = true;
        gateHandler.removeCallbacksAndMessages(null);
        btnGate.setBackgroundResource(R.drawable.bg_btn_gate_red);
        btnGate.setEnabled(false);
        final int totalSeconds = (int) Math.ceil(remainingMs / 1000.0);
        tvBtnGate.setText(getString(R.string.gate_open_countdown, totalSeconds));

        gateTick = new Runnable() {
            int remaining = totalSeconds;

            @Override
            public void run() {
                if (!isAdded()) return;
                remaining--;
                if (remaining <= 0) {
                    applyGateState(false);
                    resetGateButton();
                    return;
                }
                tvBtnGate.setText(getString(R.string.gate_open_countdown, remaining));
                gateHandler.postDelayed(this, 1000L);
            }
        };
        gateHandler.postDelayed(gateTick, 1000L);
    }

    private void resetGateButton() {
        gateHandler.removeCallbacksAndMessages(null);
        gateTick = null;
        gateBusy = false;
        btnGate.setBackgroundResource(R.drawable.bg_btn_gate_green);
        btnGate.setEnabled(true);
        tvBtnGate.setText(R.string.open_gate);
    }

    // Live data from the ESP32 — overrides whatever the mock API showed
    private void bindLive(HalaStatus st) {
        applyGateState(st.gateOpen);

        int occ = st.occupiedSpots();
        int avl = st.availableSpots;
        int total = Math.max(1, st.totalSpots);
        int pct = Math.round(occ * 100f / total);

        tvOccCount.setText(occ + " Occ");
        tvAvlCount.setText(avl + " Avl");
        tvOccPctSmall.setText(pct + "%");

        donut.setData(
                new float[]{occ, avl, 0},
                new int[]{Color.parseColor("#EF4444"), Color.parseColor("#22C55E"), Color.parseColor("#F59E0B")}
        );
        tvOccPercent.setText(pct + "%");
        tvDonutOccupied.setText(getString(R.string.occupancy_legend_occupied, occ));
        tvDonutAvailable.setText(getString(R.string.occupancy_legend_available, avl));
        tvDonutReserved.setText(getString(R.string.occupancy_legend_reserved, 0));

        // Per-spot data: spots 1-2 -> Row A, spots 3-4 -> Row B
        if (st.spotOccupied != null && st.spotOccupied.length >= 4) {
            rowAContainer.removeAllViews();
            rowBContainer.removeAllViews();
            populateRow(rowAContainer, Arrays.asList(
                    liveSpot("A1", st.spotOccupied[0]),
                    liveSpot("A2", st.spotOccupied[1])));
            populateRow(rowBContainer, Arrays.asList(
                    liveSpot("B1", st.spotOccupied[2]),
                    liveSpot("B2", st.spotOccupied[3])));
        }
    }

    private ParkingSpot liveSpot(String id, boolean occupied) {
        return new ParkingSpot(id, id.substring(0, 1),
                occupied ? ParkingSpot.Status.OCCUPIED : ParkingSpot.Status.FREE,
                Color.parseColor("#3B82F6"));
    }

    private void bind(ParkingState s) {
        if (!isAdded()) return;
        // Connected to the real gate: ignore the mock and show ESP32 data
        HalaStatus live = gateBt.getLastStatus();
        if (live != null) {
            bindLive(live);
            tvEntry.setText(getString(R.string.entry_count, s.entryCount));
            tvExit.setText(getString(R.string.exit_count, s.exitCount));
            return;
        }
        applyGateState(s.gateOpen);
        if (s.gateOpen && s.gateRemainingMs > 0L && !gateBusy) {
            startGateCountdown(s.gateRemainingMs);
        }
        tvEntry.setText(getString(R.string.entry_count, s.entryCount));
        tvExit.setText(getString(R.string.exit_count, s.exitCount));

        // Footer summary inside the dark card: 5 Occ / 5 Avl / pct based on visible row spots
        int rowOcc = countVisibleOccupied(s);
        int rowAvl = countVisibleFree(s);
        int rowTotal = Math.max(1, rowOcc + rowAvl);
        tvOccCount.setText(rowOcc + " Occ");
        tvAvlCount.setText(rowAvl + " Avl");
        tvOccPctSmall.setText(Math.round((rowOcc * 100f) / rowTotal) + "%");

        rowAContainer.removeAllViews();
        rowBContainer.removeAllViews();
        if (s.rows.size() > 0) populateRow(rowAContainer, s.rows.get(0).spots);
        if (s.rows.size() > 1) populateRow(rowBContainer, s.rows.get(1).spots);

        // Donut
        donut.setData(
                new float[]{s.occupied, s.available, s.reserved},
                new int[]{Color.parseColor("#EF4444"), Color.parseColor("#22C55E"), Color.parseColor("#F59E0B")}
        );
        tvOccPercent.setText(s.occupancyPercent() + "%");
        tvDonutOccupied.setText(getString(R.string.occupancy_legend_occupied, s.occupied));
        tvDonutAvailable.setText(getString(R.string.occupancy_legend_available, s.available));
        tvDonutReserved.setText(getString(R.string.occupancy_legend_reserved, s.reserved));
    }

    // count how many spots are taken across all rows
    private int countVisibleOccupied(ParkingState s) {
        int n = 0;
        for (ParkingRow r : s.rows) {
            for (ParkingSpot p : r.spots) {
                if (p.status == ParkingSpot.Status.OCCUPIED) {
                    n++;
                }
            }
        }
        return n;
    }

    // count how many spots are empty
    private int countVisibleFree(ParkingState s) {
        int n = 0;
        for (ParkingRow r : s.rows) {
            for (ParkingSpot p : r.spots) {
                if (p.status == ParkingSpot.Status.FREE) {
                    n++;
                }
            }
        }
        return n;
    }

    private void populateRow(LinearLayout container, java.util.List<ParkingSpot> spots) {
        Context ctx = requireContext();
        for (int i = 0; i < spots.size(); i++) {
            ParkingSpot spot = spots.get(i);
            View cell = buildSpotCell(ctx, spot);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, dp(64), 1f);
            if (i > 0) lp.setMarginStart(dp(6));
            container.addView(cell, lp);
        }
    }

    private View buildSpotCell(Context ctx, ParkingSpot spot) {
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setBackgroundResource(R.drawable.bg_spot_outline);
        col.setPadding(dp(4), dp(6), dp(4), dp(6));

        if (spot.status == ParkingSpot.Status.OCCUPIED) {
            ImageView car = new ImageView(ctx);
            car.setImageResource(R.drawable.ic_car);
            car.setColorFilter(spot.carColor, PorterDuff.Mode.SRC_IN);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(24), dp(24));
            col.addView(car, iconLp);
        } else {
            View placeholder = new View(ctx);
            col.addView(placeholder, new LinearLayout.LayoutParams(dp(24), dp(24)));
        }

        TextView label = new TextView(ctx);
        label.setText(spot.id);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        label.setTextColor(Color.parseColor("#94A3B8"));
        LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lblLp.topMargin = dp(4);
        col.addView(label, lblLp);
        return col;
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
