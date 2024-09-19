package com.example.ble;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
    private List<BluetoothDevice> deviceList;
    private List<byte[]> scanRecordList;
    private int companyId;

    public DeviceAdapter(List<BluetoothDevice> deviceList, List<byte[]> scanRecordList, int companyId) {
        this.deviceList = deviceList;
        this.scanRecordList = scanRecordList;
        this.companyId = companyId;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        BluetoothDevice device = deviceList.get(position);
        byte[] scanRecord = scanRecordList.get(position);

        holder.deviceName.setText(device.getName() != null ? device.getName() : "Unknown Device");
        holder.deviceAddress.setText(device.getAddress());

        // Check if the device has the correct company ID
        int scanCompanyId = (scanRecord[6] & 0xFF) << 8 | (scanRecord[5] & 0xFF);
        if (scanCompanyId == companyId) {
            holder.connectButton.setVisibility(View.VISIBLE);
        } else {
            holder.connectButton.setVisibility(View.GONE);
        }
        //holder.connectButton.setVisibility(View.VISIBLE);
        holder.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Notify the listener of the connect action
                if (listener != null) {
                    listener.onConnect(device, scanRecord);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        Button connectButton;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceAddress = itemView.findViewById(R.id.device_address);
            connectButton = itemView.findViewById(R.id.connect_button);
        }
    }

    public void addDevice(BluetoothDevice device, byte[] scanRecord) {
        if (!deviceList.contains(device)) {
            deviceList.add(device);
            scanRecordList.add(scanRecord);
            notifyDataSetChanged();
        }
    }
    public void clear() {
        deviceList.clear();
        scanRecordList.clear();
        notifyDataSetChanged();
    }
    private OnConnectClickListener listener;

    public interface OnConnectClickListener {
        void onConnect(BluetoothDevice device, byte[] scanRecord);
    }

    public void setOnConnectClickListener(OnConnectClickListener listener) {
        this.listener = listener;
    }

}