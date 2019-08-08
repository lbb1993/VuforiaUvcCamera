/*===============================================================================
Copyright (c) 2018 PTC Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other
countries.
===============================================================================*/

package com.vuforia.samples.uvcDriver;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.vuforia.VuforiaSamples.ui.ActivityList.ActivitySplashScreen;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public final class USBController {
    private static final String DEFAULT_USBFS = "/dev/bus/usb";
    private static final String MODULE_TAG = "Vuforia-UVCDriver";
    private static final int INVALID_VALUE = -1;

    // Any arbitrary string to match the request and the result
    public static final String REQUEST_USB_PERMISSION = "usb.premission.grant";
    public static final String ACTION_TEST = "com.lbb.test.action";

    private UsbManager mUsbManager = null;
    private UsbDevice[] mUsbDeviceList;
    private UsbDevice mUsbDevice = null;
    private UsbDeviceConnection mConnection = null;
    private HashMap<UsbDevice, CountDownLatch> mPermissionLatchMap = null;
    BroadcastReceiver broadcastReceiver;

    public USBController(final Activity activity) {
        if (activity == null) {
            return;
        }


        mUsbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> usbDeviceHashMap = mUsbManager.getDeviceList();


        mUsbDeviceList = new UsbDevice[usbDeviceHashMap.size()];
        mUsbDeviceList = usbDeviceHashMap.values().toArray(mUsbDeviceList);

        mPermissionLatchMap = new HashMap<UsbDevice, CountDownLatch>(mUsbDeviceList.length);


        for (int idx = 0; idx < mUsbDeviceList.length; idx++) {
            final UsbDevice device = mUsbDeviceList[idx];
            if (!mUsbManager.hasPermission(device)) {
//                activity.registerReceiver(broadcastReceiver, new IntentFilter(REQUEST_USB_PERMISSION));
//
//                PendingIntent permissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(REQUEST_USB_PERMISSION), 0);
//                mUsbManager.requestPermission(device, permissionIntent);

                // Put a latch for this device because we need to wait for the permission result

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                broadcastReceiver = new BroadcastReceiver() {
                                    @Override
                                    public void onReceive(Context context, Intent intent) {
                                        Log.d("USBController", "接收到授权相关广播：" + intent.getAction());
                                        if (intent.getAction().equals(REQUEST_USB_PERMISSION)) {
                                            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                                            if (device != null && mPermissionLatchMap.get(device) != null) {
                                                // We got the permission result for this device, countdown the latch
                                                mPermissionLatchMap.get(device).countDown();
                                            }
                                        }
                                    }
                                };
                                Log.d("USBController", "申请usb控制权限：" + Thread.currentThread().getId());
                                //申请权限
                                Intent intent = new Intent(REQUEST_USB_PERMISSION).addFlags(Intent. FLAG_INCLUDE_STOPPED_PACKAGES);
                                final PendingIntent mPermissionIntent = PendingIntent.getBroadcast(activity, 0, intent, 0);
                                final IntentFilter permissionFilter = new IntentFilter(REQUEST_USB_PERMISSION);
                                permissionFilter.addAction(ACTION_TEST);
                                Log.d("USBController", "注册广播接收器");
                                activity.registerReceiver(broadcastReceiver, permissionFilter);

//                                Log.d("USBController", "发送测试广播："+ Thread.currentThread().getId());
//                                activity.sendBroadcast(new Intent(USBController.ACTION_TEST));
                                Log.d("USBController", "申请权限：" + Thread.currentThread().getId());
                                mUsbManager.requestPermission(device, mPermissionIntent);
                                Log.d("USBController", "发送测试广播：" + Thread.currentThread().getId());
                                activity.sendBroadcast(new Intent(USBController.ACTION_TEST).addFlags(Intent. FLAG_INCLUDE_STOPPED_PACKAGES));

                            }
                        },2000);
                    }
                });

                mPermissionLatchMap.put(device, new CountDownLatch(1));

            }
        }
    }

    public void closeConnection() {
        if (mConnection != null) {
            mConnection.close();
            mConnection = null;
        }
    }

    public int getNumDevices() {
        return mUsbDeviceList.length;
    }

    public boolean useDevice(int index) {
        Log.d(MODULE_TAG, "useDevice(int index)java部分被调用: " + index);
        // Clear up existing device and connection
        mUsbDevice = null;
        closeConnection();

        if (index >= mUsbDeviceList.length) {
            Log.e(MODULE_TAG, "Invalid usb device index: " + index);
            return false;
        }

        UsbDevice device = mUsbDeviceList[index];

        if (mPermissionLatchMap.get(device) != null) {
            try {
                mPermissionLatchMap.get(device).await();
            } catch (InterruptedException ex) {
                Log.e(MODULE_TAG, "Error while awaiting permission latch for usb device at index " + index, ex);
                return false;
            }
        }

        if (!mUsbManager.hasPermission(device)) {
            Log.e(MODULE_TAG, "Usb permission has been denied for usb device at index " + index);
            return false;
        }

        // Assign new device and open new connection
        mUsbDevice = device;
        mConnection = mUsbManager.openDevice(mUsbDevice);

        return true;
    }

    public int getVendorId() {
        if (mUsbDevice == null) {
            return INVALID_VALUE;
        }

        return mUsbDevice.getVendorId();
    }

    public int getProductId() {
        if (mUsbDevice == null) {
            return INVALID_VALUE;
        }

        return mUsbDevice.getProductId();
    }

    public int getFileDescriptor() {
        if (mConnection == null) {
            return INVALID_VALUE;
        }

        return mConnection.getFileDescriptor();
    }

    public String getUSBFS() {
        if (mUsbDevice == null) {
            return null;
        }

        String result = null;
        final String deviceName = mUsbDevice.getDeviceName();

        final String[] deviceNameArr = !TextUtils.isEmpty(deviceName) ? deviceName.split("/") : null;
        if ((deviceNameArr != null) && (deviceNameArr.length > 2)) {
            final StringBuilder sb = new StringBuilder(deviceNameArr[0]);
            for (int i = 1; i < deviceNameArr.length - 2; i++) {
                sb.append("/").append(deviceNameArr[i]);
            }
            result = sb.toString();
        }

        if (TextUtils.isEmpty(result)) {
            return DEFAULT_USBFS;
        }

        return result;
    }

    public int getBusNumber() {
        if (mUsbDevice == null) {
            return INVALID_VALUE;
        }

        final String deviceName = mUsbDevice.getDeviceName();
        final String[] deviceNameArr = !TextUtils.isEmpty(deviceName) ? deviceName.split("/") : null;

        if (deviceNameArr == null) {
            return INVALID_VALUE;
        }

        // Bus number is the second last value of the device name
        // e.g. if device name is dev/bus/usb/001/004, the bus number is 001
        return Integer.parseInt(deviceNameArr[deviceNameArr.length - 2]);
    }

    public int getDeviceNumber() {
        if (mUsbDevice == null) {
            return INVALID_VALUE;
        }

        final String deviceName = mUsbDevice.getDeviceName();
        final String[] deviceNameArr = !TextUtils.isEmpty(deviceName) ? deviceName.split("/") : null;

        if (deviceNameArr == null) {
            return INVALID_VALUE;
        }

        // Device number is the last value of the device name
        // e.g. if device name is dev/bus/usb/001/004, the device number is 004
        return Integer.parseInt(deviceNameArr[deviceNameArr.length - 1]);
    }
}
