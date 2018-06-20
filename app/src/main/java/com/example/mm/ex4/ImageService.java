package com.example.mm.ex4;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.View;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import static java.lang.Thread.sleep;

public class ImageService extends Service {
    private BroadcastReceiver yourReceiver;

    public ImageService() {
    }

    @Override
    public void onCreate() {
        this.yourReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null) {
                    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        //get the different network states
                        if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                            startTransfer();
                            // Starting the Transfer
                        }
                    }
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.supplicant.CONNECTION_CHANGE");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        this.registerReceiver(this.yourReceiver, filter);
        return Service.START_NOT_STICKY;
    }

    private void startTransfer() {
        // Getting the Camera Folder

        Log.i("hi", "hi");

        File dcim =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "//Camera");

        if (dcim == null) {
            return;
        }

        File[] pics = dcim.listFiles();


        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel("2", "MyChannel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        if (channel == null) {
            return;
        }

        int notificationId = 1;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "2");
        builder .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Picture Transfer")
                .setContentText("Transfer in progress")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(100,0,false);
        notificationManager.notify(notificationId, builder.build());


        for (int i = 0; i< pics.length; i++)
        {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.v("name","Transfered:" + pics[i].getName() );

            builder.setProgress(pics.length,i,false);
            notificationManager.notify(notificationId, builder.build());
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        builder.setProgress(pics.length,pics.length,false);
        builder.setContentText("transfer completed!");
        notificationManager.notify(notificationId, builder.build());




        //TODO Image transfer with the progress bar and other zevel to be done

        new Thread(new Runnable() {
            @Override
            public void run() {

                File dcim =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "//Camera");

                if (dcim == null) {
                    return;
                }

                File[] pics = dcim.listFiles();

                //here you must put your computer's IP address.
                try {
                    InetAddress serverAddr = InetAddress.getByName("10.0.2.2");

                    Socket socket = null;
                    try {
                        //create a socket to make the connection with the server
                        socket = new Socket(serverAddr, 8000);

                        //sends the message to the server
                        OutputStream output = socket.getOutputStream();
                        InputStream input = socket.getInputStream();

                        FileInputStream fis = new FileInputStream(pics[3]);

                        DataOutputStream dataOut = new DataOutputStream(output);

                        Bitmap bm = BitmapFactory.decodeStream(fis);
                        byte[] imgbyte = getBytesFromBitmap(bm);
                        int imgbyteLength = imgbyte.length;
                        dataOut.writeInt(imgbyteLength);
                        //output.write(imgbyteLength);
                        output.write(imgbyte);
                        output.flush();
                        Log.v("sending","sent picture");

                    } catch (FileNotFoundException e) {
                        Log.e("TCP", "S: ERROR", e);
                    } catch (IOException e) {
                        Log.e("TCP", "S: ERROR", e);
                    } finally {
                        if (socket != null) {
                            socket.close();
                        }
                    }

                }
                catch (Exception e)
                {
                    Log.e("TCP", "C: ERROR", e);
                }

            }
        }).start();

        /*
        //here you must put your computer's IP address.
        try {
            InetAddress serverAddr = InetAddress.getByName("10.0.2.2");

            Socket socket = null;
            try {
                //create a socket to make the connection with the server
                socket = new Socket(serverAddr, 1234);

                //sends the message to the server
                OutputStream output = socket.getOutputStream();

                FileInputStream fis = new FileInputStream(pics[3]);


                Bitmap bm = BitmapFactory.decodeStream(fis);
                byte[] imgbyte = getBytesFromBitmap(bm);


                output.write(imgbyte);
                output.flush();

            } catch (FileNotFoundException e) {
                Log.e("TCP", "S: ERROR", e);
            } catch (IOException e) {
                Log.e("TCP", "S: ERROR", e);
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }

        }
        catch (Exception e)
        {
            Log.e("TCP", "C: ERROR", e);
        }

    */

    }
    @Override
    public void onDestroy() {
        unregisterReceiver(yourReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 70, stream);
        return stream.toByteArray();
    }




}
