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
import java.util.ArrayList;

import static java.lang.Thread.sleep;

public class ImageService extends Service {
    private BroadcastReceiver yourReceiver;
    private boolean receiverRegistered;

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
        receiverRegistered = true;
        return Service.START_NOT_STICKY;
    }

    private void startTransfer() {

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

        //initializing the progress bar
        final int notificationId = 1;
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "2");
        builder .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Image Transfer")
                .setContentText("Transfer in progress")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setProgress(100,0,false);
        notificationManager.notify(notificationId, builder.build());

        new Thread(new Runnable() {
            @Override
            public void run() {

                // Getting the Camera Folder
                File dcim =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

                if (dcim == null) {
                    return;
                }

                //getting the images from DCIM recursively
                ArrayList<File> pictures = new ArrayList<File>();
                getPics(dcim,pictures);
                if(pictures.size() < 1) return;
                File[] pics = new File[pictures.size()];
                pics = pictures.toArray(pics);

                //connecting to the server
                try {
                    InetAddress serverAddr = InetAddress.getByName("10.0.2.2");

                    Socket socket = null;
                    try {
                        //create a socket to make the connection with the server
                        socket = new Socket(serverAddr, 8000);
                        //sends the message to the server
                        OutputStream output = socket.getOutputStream();
                        DataOutputStream dataOut = new DataOutputStream(output);
                        InputStream input = socket.getInputStream();

                        //sending the images
                        for(int i=0;i<pics.length;i++) {

                            //updating the progress bar
                            builder.setProgress(pics.length,i,false);
                            notificationManager.notify(notificationId, builder.build());

                            String picName = pics[i].getName();
                            int nameLength = picName.length();
                            dataOut.writeInt(nameLength);
                            dataOut.writeBytes(picName);
                            FileInputStream fis = new FileInputStream(pics[i]);
                            Bitmap bm = BitmapFactory.decodeStream(fis);
                            byte[] imgbyte = getBytesFromBitmap(bm);
                            int imgbyteLength = imgbyte.length;
                            Log.v(getClass().getName(), String.format("value = %d", imgbyteLength));
                            dataOut.writeInt(imgbyteLength);
                            output.write(imgbyte);
                            output.flush();
                            Thread.sleep(1000);
                        }

                        //display completed message
                        builder.setProgress(pics.length,pics.length,false);
                        builder.setContentText("transfer completed!");
                        notificationManager.notify(notificationId, builder.build());

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

    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 70, stream);
        return stream.toByteArray();
    }

    public static void getPics(File dir, ArrayList<File> pics) {
        File[] files = dir.listFiles();

        for (File file : files) {
            if (file.isDirectory()

                    //makes no sense to get thumbnails and then create new thumbnails from them,
                    //as the ImageService do to all the photos that are sent to it
                    && !file.getName().endsWith("thumbnails")
                    && !file.getName().endsWith("Thumbnails")) {
                getPics(file,pics);
            }
            else if(file.getName().endsWith(".jpg")
                    || file.getName().endsWith(".png")
                    || file.getName().endsWith(".gif")
                    || file.getName().endsWith(".bmp")){
                    pics.add(file);
                }
            }
    }
}
