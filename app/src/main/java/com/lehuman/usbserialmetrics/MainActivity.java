package com.lehuman.usbserialmetrics;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.lehuman.usbserialmetrics.databinding.ActivityMainBinding;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    Metric metric = new Metric();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();

        binding.logView.setMovementMethod(new ScrollingMovementMethod());
        binding.mik3yBtn.setOnClickListener(view -> {
            metric.reset();
            metric.start();
            binding.MetricText.setText(metric.toString());
            mik3yImplement();
        });
        binding.felHR85Btn.setOnClickListener(view -> {
            metric.reset();
            metric.start();
            binding.MetricText.setText(metric.toString());
            felHR85Implement();
        });

        Runnable run = new Runnable() {
            @Override
            public void run() {
                binding.MetricText.setText(metric.toString());
                binding.MetricText.postDelayed(this, 250);
            }
        };
        binding.MetricText.postDelayed(run, 250);
        binding.logView.setText("");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        hideUI();
    }

    private void setupUI() {
        // Account for notches in newer phones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        hideUI();
    }

    private void hideUI() {
        // Hide Action bar and navigation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getInsetsController().hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    private static final String ACTION_USB_PERMISSION = "com.lehuman.usbserialmetrics.USB_PERMISSION";

    private void requestUserPermission(UsbManager manager, UsbDevice device) {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        manager.requestPermission(device, mPendingIntent);
    }

    void log(String ID, String msg) {
        if (binding.logView.getLineCount() > 500) {
            binding.logView.setText("");
        }
        binding.logView.append(String.format("\n[%s] %s", ID, msg));
    }

    boolean visible = false;

    void receive(String ID, byte[] msg) {
        visible = !visible;
        binding.Blinker.post(() -> {
            binding.Blinker.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            if (!visible)
                binding.ErrBlinker.setVisibility(View.INVISIBLE);
        });
        metric.newValue(msg);
    }

    void error(String ID, String msg) {
        log(ID, "ERR: " + msg);
        binding.ErrBlinker.post(() -> binding.ErrBlinker.setVisibility(View.VISIBLE));
    }

    void mik3yImplement() {
        String ID = "mik3y";

        log(ID, "Starting mik3y implement");

        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            error(ID, "Driver list empty");
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            log(ID, "Requesting permissions");
            requestUserPermission(manager, driver.getDevice());
            return;
        }

        UsbSerialPort port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try {
            port.open(connection);
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            error(ID, "Failed to open");
            e.printStackTrace();
            return;
        }

        SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, new SerialInputOutputManager.Listener() {
            @Override
            public void onNewData(byte[] data) {
                runOnUiThread(() -> receive(ID, data));
            }

            @Override
            public void onRunError(Exception e) {
                error(ID, "Running error");
            }
        });

        usbIoManager.start();

        log(ID, "Manager started");
    }

    void felHR85Implement() {

        String ID = "felHR85";

        log(ID, "Starting felHR85 implement");

        UsbDevice device = null;
        UsbDeviceConnection connection = null;

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> usbDevices = manager.getDeviceList();

        if (!usbDevices.isEmpty()) {
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                if (UsbSerialDevice.isSupported(device)) {
                    connection = manager.openDevice(device);
                    if (connection == null) {
                        log(ID, "Requesting permissions");
                        requestUserPermission(manager, device);
                        return;
                    }
                }
            }
        }

        if (device == null || connection == null) {
            error(ID, "Failed to open");
            return;
        }

        UsbSerialDevice serial = UsbSerialDevice.createUsbSerialDevice(device, connection);

        serial.open();
        serial.setBaudRate(115200);
        serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
        serial.setParity(UsbSerialInterface.PARITY_ODD);
        serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

        UsbSerialInterface.UsbReadCallback mCallback = data -> receive(ID, data);

        serial.read(mCallback);
        log(ID, "Manager started");
    }

}