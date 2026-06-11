package com.example.floginsignup.bluetooth;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import com.example.floginsignup.R;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class MainActivity2 extends AppCompatActivity {

    private String deviceName = null;
    private String deviceAddress;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;

    private final static int CONNECTING_STATUS = 1;
    private final static int MESSAGE_READ = 2;

    // ============================================================
    // HALA State Tracking
    // ============================================================
    private boolean gateIsOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        Utils utils = Utils.getInstance(this);

        // --- UI Initialization ---
        final Button buttonConnect  = findViewById(R.id.buttonConnect);
        final Toolbar toolbar       = findViewById(R.id.toolbar);
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        final TextView textViewInfo = findViewById(R.id.textViewInfo);
        final Button buttonToggle   = findViewById(R.id.buttonToggle);
        final ImageView imageView   = findViewById(R.id.imageView);

        progressBar.setVisibility(View.GONE);
        buttonToggle.setEnabled(false);
        buttonToggle.setText("OPEN GATE");  // ✅ HALA label (was "TURN ON")
        imageView.setBackgroundColor(getResources().getColor(R.color.colorOff));
        textViewInfo.setText("Not connected to HALA system");

        // --- Connect to device if coming from SelectDeviceActivity ---
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null) {
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            toolbar.setSubtitle("Connecting to " + deviceName + "...");
            progressBar.setVisibility(View.VISIBLE);
            buttonConnect.setEnabled(false);

            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter, deviceAddress);
            createConnectThread.start();
        }

        // ============================================================
        // Bluetooth Handler — receives status updates and ESP32 messages
        // ============================================================
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {

                    // --- Connection status updates ---
                    case CONNECTING_STATUS:
                        switch (msg.arg1) {
                            case 1: // Connected successfully
                                toolbar.setSubtitle("Connected to " + deviceName);
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                buttonToggle.setEnabled(true);
                                textViewInfo.setText("Connected. Waiting for HALA status...");
                                break;
                            case -1: // Failed to connect
                                toolbar.setSubtitle("Connection failed");
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                textViewInfo.setText("Failed to connect. Try again.");
                                break;
                        }
                        break;

                    // --- Messages received from ESP32 ---
                    case MESSAGE_READ:
                        String arduinoMsg = msg.obj.toString().trim();
                        Log.d("HALA_MSG", "Received: " + arduinoMsg);

                        // ✅ Parse HALA protocol: HALA|S:x/4|G:x|L:x
                        if (arduinoMsg.startsWith("HALA|")) {
                            parseHalaStatus(arduinoMsg, textViewInfo, imageView, buttonToggle);

                            // ✅ Handle plain gate event messages from ESP32
                        } else if (arduinoMsg.equalsIgnoreCase("GATE:OPEN")) {
                            gateIsOpen = true;
                            imageView.setBackgroundColor(getResources().getColor(R.color.colorOn));
                            buttonToggle.setText("CLOSE GATE");
                            textViewInfo.setText("Gate is OPEN");

                        } else if (arduinoMsg.equalsIgnoreCase("GATE:CLOSED")) {
                            gateIsOpen = false;
                            imageView.setBackgroundColor(getResources().getColor(R.color.colorOff));
                            buttonToggle.setText("OPEN GATE");
                            textViewInfo.setText("Gate is CLOSED");

                        } else if (arduinoMsg.equalsIgnoreCase("HALA SYSTEM READY")) {
                            textViewInfo.setText("HALA System is READY");

                        } else {
                            // Show any other raw message for debugging
                            textViewInfo.setText("ESP32: " + arduinoMsg);
                        }
                        break;
                }
            }
        };

        // --- Connect button: go to device selection screen ---
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity2.this, SelectDeviceActivity.class);
                startActivity(intent);
            }
        });

        // ✅ Gate toggle button — sends "O" to open, "C" to close
        buttonToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connectedThread == null) {
                    textViewInfo.setText("Not connected to HALA system");
                    return;
                }

                String btnState = buttonToggle.getText().toString().toLowerCase();
                switch (btnState) {
                    case "open gate":
                        connectedThread.write("O"); // ✅ Matches ESP32: cmd == 'O' → openGate()
                        textViewInfo.setText("Command sent: OPEN GATE");
                        break;
                    case "close gate":
                        connectedThread.write("C"); // ✅ Matches ESP32: cmd == 'C' → closeGate()
                        textViewInfo.setText("Command sent: CLOSE GATE");
                        break;
                }
            }
        });
    }

    // ============================================================
    // Parse HALA Status String: "HALA|S:3/4|G:1|L:D"
    // ============================================================
    private void parseHalaStatus(String msg, TextView textViewInfo,
                                 ImageView imageView, Button buttonToggle) {
        try {
            // Extract available spots: S:x/4
            int spotsAvailable = -1;
            if (msg.contains("S:")) {
                int sIdx = msg.indexOf("S:") + 2;
                int slashIdx = msg.indexOf("/", sIdx);
                if (slashIdx > sIdx) {
                    spotsAvailable = Integer.parseInt(msg.substring(sIdx, slashIdx));
                }
            }

            // Extract gate status: G:1 = open, G:0 = closed
            boolean gateOpen = msg.contains("G:1");

            // Extract lighting: L:N = night, L:D = day
            boolean isNight = msg.contains("L:N");

            // Update gate state
            gateIsOpen = gateOpen;

            // Update UI colors based on gate state
            if (gateOpen) {
                imageView.setBackgroundColor(getResources().getColor(R.color.colorOn));
                buttonToggle.setText("CLOSE GATE");
            } else {
                imageView.setBackgroundColor(getResources().getColor(R.color.colorOff));
                buttonToggle.setText("OPEN GATE");
            }

            // Build readable status string
            String spotText  = (spotsAvailable >= 0) ? spotsAvailable + "/4 spots free" : "? spots";
            String gateText  = gateOpen  ? "OPEN"  : "CLOSED";
            String lightText = isNight   ? "Night" : "Day";

            textViewInfo.setText(
                    "HALA | Spots: " + spotText +
                            " | Gate: " + gateText +
                            " | " + lightText + " mode"
            );

        } catch (Exception e) {
            Log.e("HALA_PARSE", "Failed to parse: " + msg, e);
            textViewInfo.setText("Raw: " + msg);
        }
    }

    /* ==================== Thread: Create Bluetooth Connection ==================== */
    public static class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;

            Utils u = Utils.getInstance();
            if (ActivityCompat.checkSelfPermission(u.getMainAct(),
                    android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();
            try {
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Utils u = Utils.getInstance();

            if (ActivityCompat.checkSelfPermission(u.getMainAct(),
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(u.getMainAct(),
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
            }

            try {
                mmSocket.connect();
                Log.e("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /* ==================== Thread: Data Transfer ==================== */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn  = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error getting streams", e);
            }
            mmInStream  = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes = 0;
            while (true) {
                try {
                    buffer[bytes] = (byte) mmInStream.read();
                    if (buffer[bytes] == '\n') {
                        String readMessage = new String(buffer, 0, bytes).trim();
                        Log.e("ESP32 Message", readMessage);
                        handler.obtainMessage(MESSAGE_READ, readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        // Send a command string to the ESP32
        public void write(String input) {
            byte[] bytes = input.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error", "Unable to send message", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close socket", e);
            }
        }
    }

    /* ==================== Clean up on Back Press ==================== */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (createConnectThread != null) {
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}