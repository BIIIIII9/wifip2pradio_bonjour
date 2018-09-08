

package com.example.a233.bluetooth_radio;



import android.bluetooth.BluetoothAdapter;

import java.lang.reflect.Method;

public abstract class MyBluetoothMethodManager {

    public static boolean isBluetoothSupported(){
        return BluetoothAdapter.getDefaultAdapter() != null;
    }
    public static boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }
    public static boolean backStageTurnOnBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter != null && bluetoothAdapter.enable();
    }
    //Keep discoverable of Bluetooth constantly
    public static void openDiscoverable() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(adapter, 300000);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 300000);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    //Stop to keep discoverable of Bluetooth constantly
    public static void closeDiscoverable() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(adapter, 1);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void starBluetoothDeviceSearch(){
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.startDiscovery();
    }
    public static void stopBluetoothDeviceSearch(){
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.cancelDiscovery();
    }
}