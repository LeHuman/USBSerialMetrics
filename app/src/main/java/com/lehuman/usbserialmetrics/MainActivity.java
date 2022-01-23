package com.lehuman.usbserialmetrics;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.lehuman.usbserialmetrics.databinding.ActivityMainBinding;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    Metric metric = new Metric();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.logView.setMovementMethod(new ScrollingMovementMethod());
        binding.mik3yBtn.setOnClickListener(view -> {
            metric.reset();
            metric.start();
            mik3yImplement();
        });
        binding.felHR85Btn.setOnClickListener(view -> {
            metric.reset();
            metric.start();
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
        binding.logView.append(String.format("[%s] %s\n", ID, msg));
    }

    boolean visible = false;

    void receive(String ID, byte[] msg) {
//        binding.logView.append(String.format("(%s) %s\n", ID, new String(msg).replace("\n", "\\n").replace("\t", "\\t")));
        visible = !visible;
        binding.Blinker.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        metric.newValue(msg);
    }

    void mik3yImplement() {
        String ID = "mik3y";

        log(ID, "Starting mik3y implement");

        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            log(ID, "Driver list empty");
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
            log(ID, "Failed to open");
            e.printStackTrace();
        }

        SerialInputOutputManager usbIoManager = new SerialInputOutputManager(port, new SerialInputOutputManager.Listener() {
            @Override
            public void onNewData(byte[] data) {
                runOnUiThread(() -> receive(ID, data));
            }

            @Override
            public void onRunError(Exception e) {
                log(ID, "Running error");
            }
        });

        usbIoManager.start();

        log(ID, "Manager started");
    }

    void felHR85Implement() {

        String ID = "felHR85";

        log(ID, "Starting felHR85 implement");

//        UsbDevice device = null;
//        UsbDeviceConnection usbConnection;
//
//        String ACTION_NO_USB = "com.felhr.usbservice.NO_USB";
//
//        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
//        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
//        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
//        if (!usbDevices.isEmpty()) {
//
//            // first, dump the hashmap for diagnostic purposes
//            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
//                device = entry.getValue();
//            }
//
//            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
//                device = entry.getValue();
//
//                if (UsbSerialDevice.isSupported(device)) {
//                    // There is a supported device connected - request permission to access it.
//        log(ID, "Requesting permissions");
//                    requestUserPermission(usbManager, device);
//                    break;
//                } else {
//                    usbConnection = null;
//                    device = null;
//                }
//            }
//            if (device == null) {
//                // There are no USB devices connected (but usb host were listed). Send an intent to MainActivity.
//                Intent intent = new Intent(ACTION_NO_USB);
//                sendBroadcast(intent);
//            }
//        } else {
//            // There is no USB devices connected. Send an intent to MainActivity
//            Intent intent = new Intent(ACTION_NO_USB);
//            sendBroadcast(intent);
//        }
//
//        UsbSerialDevice serial = UsbSerialDevice.createUsbSerialDevice(device, usbConnection);
//
//        serial.open();
//        serial.setBaudRate(115200);
//        serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
//        serial.setParity(UsbSerialInterface.PARITY_ODD);
//        serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
//
//        UsbSerialInterface.UsbReadCallback mCallback = data -> {
//            receive(ID, data);
//        };
//
//        serial.read(mCallback);
    }

}