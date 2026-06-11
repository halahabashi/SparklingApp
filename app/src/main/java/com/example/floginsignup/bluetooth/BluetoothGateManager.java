package com.example.floginsignup.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * App-wide Bluetooth connection to the HALA gate controller (ESP32).
 * Protocol: send "O" to open the gate, "C" to close it.
 * The ESP32 streams newline-terminated status messages back
 * (e.g. "HALA|S:3/4|G:1|L:D", "GATE:OPEN", "GATE:CLOSED").
 */
public class BluetoothGateManager {

    private static final String TAG = "BluetoothGateManager";
    // Standard Serial Port Profile UUID used by ESP32 BluetoothSerial
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final String CMD_OPEN_GATE = "O";
    public static final String CMD_CLOSE_GATE = "C";

    public interface Listener {
        default void onConnected(String deviceName) {}
        default void onConnectionFailed(String reason) {}
        default void onDisconnected() {}
        default void onMessage(String message) {}
        /** Fired for every parsed "HALA|..." status line from the ESP32. */
        default void onStatus(HalaStatus status) {}
    }

    private static BluetoothGateManager instance;

    public static synchronized BluetoothGateManager getInstance() {
        if (instance == null) instance = new BluetoothGateManager();
        return instance;
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private BluetoothSocket socket;
    private OutputStream outStream;
    private Thread readThread;
    private volatile boolean connected = false;
    private String deviceName;
    private final java.util.List<Listener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private volatile HalaStatus lastStatus;

    private BluetoothGateManager() {}

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public boolean isConnected() {
        return connected;
    }

    public String getDeviceName() {
        return deviceName;
    }

    /** Latest parsed status from the ESP32, or null if none received yet. */
    public HalaStatus getLastStatus() {
        return lastStatus;
    }

    /**
     * Connects to the given paired device. Caller must already hold the
     * BLUETOOTH_CONNECT runtime permission.
     */
    @SuppressLint("MissingPermission")
    public void connect(String name, String address) {
        ioExecutor.execute(() -> {
            closeSocketQuietly();
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter == null) {
                    notifyFailed("Bluetooth not available on this device");
                    return;
                }
                try {
                    // Speeds up the connection but needs BLUETOOTH_SCAN, which
                    // the app doesn't request — skip it if not allowed
                    adapter.cancelDiscovery();
                } catch (SecurityException ignored) {
                }
                BluetoothDevice device = adapter.getRemoteDevice(address);
                BluetoothSocket s = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                s.connect();

                socket = s;
                outStream = s.getOutputStream();
                connected = true;
                deviceName = name;
                startReadLoop(s.getInputStream());

                mainHandler.post(() -> {
                    for (Listener l : listeners) l.onConnected(name);
                });
            } catch (IOException | SecurityException e) {
                Log.e(TAG, "Failed to connect to " + name, e);
                closeSocketQuietly();
                notifyFailed(e.getMessage() != null ? e.getMessage() : "Connection failed");
            }
        });
    }

    public void openGate() {
        send(CMD_OPEN_GATE);
    }

    public void closeGate() {
        send(CMD_CLOSE_GATE);
    }

    public void send(String command) {
        ioExecutor.execute(() -> {
            if (!connected || outStream == null) {
                Log.w(TAG, "Not connected; dropping command: " + command);
                return;
            }
            try {
                outStream.write(command.getBytes());
                outStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Write failed", e);
                handleDisconnect();
            }
        });
    }

    public void disconnect() {
        ioExecutor.execute(this::handleDisconnect);
    }

    private void startReadLoop(InputStream in) {
        readThread = new Thread(() -> {
            StringBuilder line = new StringBuilder();
            try {
                int c;
                while ((c = in.read()) != -1) {
                    if (c == '\n') {
                        String msg = line.toString().trim();
                        line.setLength(0);
                        if (!msg.isEmpty()) {
                            Log.d(TAG, "Received: " + msg);
                            HalaStatus status = HalaStatus.parse(msg);
                            if (status != null) lastStatus = status;
                            mainHandler.post(() -> {
                                for (Listener l : listeners) {
                                    l.onMessage(msg);
                                    if (status != null) l.onStatus(status);
                                }
                            });
                        }
                    } else {
                        line.append((char) c);
                    }
                }
            } catch (IOException e) {
                Log.d(TAG, "Read loop ended: " + e.getMessage());
            }
            handleDisconnect();
        }, "bt-gate-read");
        readThread.start();
    }

    private void handleDisconnect() {
        boolean wasConnected = connected;
        closeSocketQuietly();
        lastStatus = null;
        if (wasConnected) {
            mainHandler.post(() -> {
                for (Listener l : listeners) l.onDisconnected();
            });
        }
    }

    private void closeSocketQuietly() {
        connected = false;
        outStream = null;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            socket = null;
        }
    }

    private void notifyFailed(String reason) {
        mainHandler.post(() -> {
            for (Listener l : listeners) l.onConnectionFailed(reason);
        });
    }
}
