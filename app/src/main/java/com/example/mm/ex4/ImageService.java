package com.example.mm.ex4;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.File;

public class ImageService extends Service {
    private BroadcastReceiver yourReceiver;

    public ImageService() {
    }

    @Override
    public void onCreate()
    {
        this.yourReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null)
                {
                    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI)
                    {
                        //get the different network states
                        if (networkInfo.getState() == NetworkInfo.State.CONNECTED)
                        {
                            startTransfer();
                            // Starting the Transfer
                        }
                    }
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        final IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.supplicant.CONNECTION_CHANGE");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        this.registerReceiver(this.yourReceiver, filter);
        return Service.START_NOT_STICKY;
    }
    private void startTransfer()
    {

        int r = 90;
        // Getting the Camera Folder

        Log.i("hi","hi");

        File dcim =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "//Camera");

        if (dcim == null) {
            return;
        }




        File[] pics = dcim.listFiles();
        int cnt = 0;
        for (File pic : pics) {
            Log.v("name",pic.getName() );
        }


        /*
        if (pics != null) {
            for (File pic : pics) {
                Log.i("forLoop", "inside for");
            }
        }
        else
            {
                Log.i("check", "pics is null");
            }

*/


        //TODO Image transfer with the progress bar and other zevel to be done
    }

    @Override
    public void onDestroy()
    {
        unregisterReceiver(yourReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
