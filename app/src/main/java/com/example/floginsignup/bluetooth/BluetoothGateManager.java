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
        void onConnected(String deviceName);
        void onConnectionFailed(String reason);
        void onDisconnected();
        void onMessage(String message);
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
    private Listener listener;

    private BluetoothGateManager() {}

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getDeviceName() {
        return deviceName;
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
                adapter.cancelDiscovery();
                BluetoothDevice device = adapter.getRemoteDevice(address);
                BluetoothSocket s = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                s.connect();

                socket = s;
                outStream = s.getOutputStream();
                connected = true;
                deviceName = name;
                startReadLoop(s.getInputStream());

                mainHandler.post(() -> {
                    if (listener != null) listener.onConnected(name);
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
                            mainHandler.post(() -> {
                                if (listener != null) listener.onMessage(msg);
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
        if (wasConnected) {
            mainHandler.post(() -> {
                if (listener != null) listener.onDisconnected();
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
            if (listener != null) listener.onConnectionFailed(reason);
        });
    }
}
