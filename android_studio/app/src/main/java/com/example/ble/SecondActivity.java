package com.example.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Collections;
import java.util.List;

public class SecondActivity extends AppCompatActivity {

    private static final int MANUFACTURER_ID = 0x0402FF;
    private static final String TAG = "BLEApp";
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothDevice connectedDevice;
    private TextView textFlags, textManufacturerData, textTemperature, textHumidity, device;
    private Button buttonDisconnect;
    private String name, s_temperature, s_humidity;
    private byte[] scanRecord;
    private static final byte ENCRYPTION_KEY = (byte) 0xAB;
    private Handler scanHandler = new Handler();
    private Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            startScanning();
            scanHandler.postDelayed(this, 5000); // Quét lại sau 5 giây
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        // Ánh xạ các thành phần UI từ layout activity_device.xml
        textFlags = findViewById(R.id.flag_value);
        textManufacturerData = findViewById(R.id.manufacturer_data_value);
        textTemperature = findViewById(R.id.temperature_value);
        textHumidity = findViewById(R.id.humidity_value);
        device = findViewById(R.id.text_deviceName);
        buttonDisconnect = findViewById(R.id.button_disconnect);

        // Lấy dữ liệu từ intent (nếu có)
        scanRecord = getIntent().getByteArrayExtra("SCAN_RECORD");
        name = getIntent().getStringExtra("NAME");

        if (name != null) {
            device.setText("Device Name: \n" + name);
        }

        if (scanRecord != null) {
            int flag = (scanRecord[0] & 0xFF) | ((scanRecord[1] & 0xFF) << 8) | ((scanRecord[2] & 0xFF) << 16);
            String s_flag = String.format("%06x", flag);
            int manufacturerData = (scanRecord[3] & 0xFF) | ((scanRecord[4] & 0xFF) << 8) | ((scanRecord[5] & 0xFF) << 16) | ((scanRecord[6] & 0xFF) << 24);
            String s_manufacturerData = String.format("%08x", manufacturerData);
            textFlags.setText("Flag: " + s_flag.toUpperCase());
            textManufacturerData.setText("Manufacturer Data: " + s_manufacturerData.toUpperCase());
            textTemperature.setText("Temperature: 0 °C");
            textHumidity.setText("Humidity: 0%");
        }

        // Thiết lập sự kiện click cho buttonDisconnect
        buttonDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectDevice();
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        // Request necessary permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, PERMISSION_REQUEST_CODE);
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not available or not enabled.");
            return;
        }

        scanHandler.post(scanRunnable); // Bắt đầu quét
        updateButtonStates();
    }

    private void startScanning() {
        if (bluetoothLeScanner != null) {
            ScanFilter scanFilter = new ScanFilter.Builder().setDeviceName(name).build();
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            bluetoothLeScanner.startScan(Collections.singletonList(scanFilter), scanSettings, scanCallback);
            Toast.makeText(this, "Scanning for new data...", Toast.LENGTH_SHORT).show();

            // Optional: Stop scan after a certain duration to save battery
            new Handler().postDelayed(() -> bluetoothLeScanner.stopScan(scanCallback), 1000);
        }
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result.getDevice().getName() != null && result.getDevice().getName().equals(name)) {
                scanRecord = result.getScanRecord().getBytes();
                updateSensorData(scanRecord);
                Toast.makeText(SecondActivity.this, "Data updated", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(SecondActivity.this, "Scan failed with error: " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    private void updateSensorData(byte[] scanRecord) {
        int temperature = extractTemperature(scanRecord);
        int humidity = extractHumidity(scanRecord);

        temperature = ((byte) temperature ^ ENCRYPTION_KEY) & 0xFF;
        humidity = ((byte) humidity ^ ENCRYPTION_KEY) & 0xFF;
        s_temperature = Integer.toString(temperature);
        s_humidity = Integer.toString(humidity);
        textTemperature.setText("Temperature: " + s_temperature + "°C");
        textHumidity.setText("Humidity: " + s_humidity + "%");
    }

    private int extractTemperature(byte[] scanRecord) {
        return (scanRecord[7] & 0xFF);
    }

    private int extractHumidity(byte[] scanRecord) {
        return (scanRecord[8] & 0xFF);
    }
    private void disconnectDevice() {
        stopScanning(); // Dừng quá trình quét
        Toast.makeText(this, "Disconnected from device", Toast.LENGTH_SHORT).show();
        finish(); // Quay về activity trước đó
    }

    private void stopScanning() {
        if (bluetoothLeScanner != null && scanCallback != null) {
            bluetoothLeScanner.stopScan(scanCallback);
            scanHandler.removeCallbacks(scanRunnable); // Dừng lặp lại quét
            Toast.makeText(this, "Stopped scanning", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateButtonStates() {
        buttonDisconnect.setBackgroundColor(Color.BLUE);
        buttonDisconnect.setTextColor(Color.WHITE);
    }
}
