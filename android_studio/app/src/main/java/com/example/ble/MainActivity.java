package com.example.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final int COMPANY_ID = 0x02FF; // Thay đổi theo companyID của bạn

    private BluetoothAdapter bluetoothAdapter;
    private DeviceAdapter deviceAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;
    private Button scanButton, resetButton;
    private Button stopScanButton;
    private boolean isScanning = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        deviceAdapter = new DeviceAdapter(new ArrayList<>(), new ArrayList<>(), COMPANY_ID);
        recyclerView.setAdapter(deviceAdapter);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        scanButton = findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermissions()) {
                    scanLeDevice(true);
                }
            }
        });

        stopScanButton = findViewById(R.id.button_stop_scan);
        stopScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(false);
            }
        });

        deviceAdapter.setOnConnectClickListener(new DeviceAdapter.OnConnectClickListener() {
            @Override
            public void onConnect(BluetoothDevice device, byte[] scanRecord) {
                connectToDevice(device, scanRecord);
            }
        });
        resetButton = findViewById(R.id.button_reset);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetDeviceList();
            }
        });
        resetButton.setBackgroundColor(Color.BLUE);
        resetButton.setTextColor(Color.WHITE);
        updateButtonStates(false);
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                }, REQUEST_PERMISSIONS);
                return false;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
                return false;
            }
        }
        return true;
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            if (bluetoothLeScanner == null) {
                Toast.makeText(this, "Failed to get BluetoothLeScanner", Toast.LENGTH_SHORT).show();
                return;
            }

            scanCallback = new ScanCallback() {

                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    handleScanResult(result);
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                    for (ScanResult result : results) {
                        handleScanResult(result);
                    }
                }

                private void handleScanResult(ScanResult result) {
                    BluetoothDevice device = result.getDevice();
                    byte[] scanRecord = result.getScanRecord().getBytes();
                    deviceAdapter.addDevice(device, scanRecord);
                }


                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    Log.e("MainActivity", "onScanFailed: " + errorCode);
                }

            };

            bluetoothLeScanner.startScan(scanCallback);
            updateButtonStates(true);
        } else {
            if (bluetoothLeScanner != null && scanCallback != null) {
                bluetoothLeScanner.stopScan(scanCallback);
                scanCallback = null;
            }
            updateButtonStates(false);
        }
    }
    private void resetDeviceList() {
        deviceAdapter.clear(); // Clear the device list in the adapter
    }
    private void updateButtonStates(boolean isScanning) {
        scanButton.setEnabled(!isScanning);
        stopScanButton.setEnabled(isScanning);
        scanButton.setBackgroundColor(isScanning ? Color.GRAY : Color.GREEN);
        stopScanButton.setBackgroundColor(isScanning ? Color.RED : Color.GRAY);
    }

    private void connectToDevice(BluetoothDevice device, byte[] scanRecord) {
        Intent intent = new Intent(MainActivity.this, SecondActivity.class);
        intent.putExtra("SCAN_RECORD", scanRecord);
        String name = device.getName() != null ? device.getName() : "Unknown Device";
        intent.putExtra("NAME", name);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanLeDevice(true);
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}