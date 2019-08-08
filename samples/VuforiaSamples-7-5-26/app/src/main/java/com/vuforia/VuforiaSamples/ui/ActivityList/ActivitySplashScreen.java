/*===============================================================================
Copyright (c) 2016-2018 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.VuforiaSamples.ui.ActivityList;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.vuforia.Vuforia;
import com.vuforia.VuforiaSamples.R;
import com.vuforia.samples.uvcDriver.USBController;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import static com.vuforia.samples.uvcDriver.USBController.ACTION_TEST;


/**
 * This class displays the splash screen and after some time will automatically display the
 * Vuforia features list
 */
public class ActivitySplashScreen extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long SPLASH_MILLIS = 450;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        RelativeLayout layout = (RelativeLayout) View.inflate(this, R.layout.splash_screen, null);

        addContentView(layout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        final Handler handler = new Handler();

        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(ActivitySplashScreen.this,
                        ActivityLauncher.class);
             /*   new Thread(){
                    @Override
                    public void run() {
                        USBController(ActivitySplashScreen.this);
                    }
                }.start();*/
                startActivity(intent);
            }

        }, SPLASH_MILLIS);


    }


    // Any arbitrary string to match the request and the result
    private static final String REQUEST_USB_PERMISSION = "usb.premission.grant";

    private UsbManager mUsbManager = null;
    private UsbDevice[] mUsbDeviceList;
    private UsbDevice mUsbDevice = null;
    private UsbDeviceConnection mConnection = null;
    private HashMap<UsbDevice, CountDownLatch> mPermissionLatchMap = null;
    BroadcastReceiver broadcastReceiver;

    public void USBController(Activity activity) {
        if (activity == null) {
            return;
        }
        Log.d("splash", "执行" + activity.getClass().getSimpleName());

        mUsbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> usbDeviceHashMap = mUsbManager.getDeviceList();

        mUsbDeviceList = new UsbDevice[usbDeviceHashMap.size()];
        mUsbDeviceList = usbDeviceHashMap.values().toArray(mUsbDeviceList);

        mPermissionLatchMap = new HashMap<UsbDevice, CountDownLatch>(mUsbDeviceList.length);


        for (int idx = 0; idx < mUsbDeviceList.length; idx++) {
            UsbDevice device = mUsbDeviceList[idx];
            if (!mUsbManager.hasPermission(device)) {
//                activity.registerReceiver(broadcastReceiver, new IntentFilter(REQUEST_USB_PERMISSION));
//
//                PendingIntent permissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent(REQUEST_USB_PERMISSION), 0);
//                mUsbManager.requestPermission(device, permissionIntent);

                // Put a latch for this device because we need to wait for the permission result

                broadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Log.d("splash", "接收到授权相关广播"+intent.getAction());
                        if (intent.getAction().equals(REQUEST_USB_PERMISSION)) {
                            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                            if (device != null && mPermissionLatchMap.get(device) != null) {
                                // We got the permission result for this device, countdown the latch
                                mPermissionLatchMap.get(device).countDown();
                            }
                        }
                    }
                };


                //申请权限
                Intent intent = new Intent(REQUEST_USB_PERMISSION);
                PendingIntent mPermissionIntent = PendingIntent.getBroadcast(activity, 0, intent, 0);
                IntentFilter permissionFilter = new IntentFilter(REQUEST_USB_PERMISSION);
                permissionFilter.addAction(ACTION_TEST);
                Log.d("splash", "注册广播接收器");
                activity.registerReceiver(broadcastReceiver, permissionFilter);

                mUsbManager.requestPermission(device, mPermissionIntent);
                Log.d("USBController", "发送测试广播");
                activity.sendBroadcast(new Intent(ACTION_TEST));

                mPermissionLatchMap.put(device, new CountDownLatch(1));

            }
        }
    }

}
