package com.example.mm.ex4;

import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startService(View view) {
        int a = 5;
        Toast.makeText(getApplicationContext(), "Service Should start.",
                Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, ImageService.class);
        startService(intent);
        Toast.makeText(getApplicationContext(), "Service Started.",
                Toast.LENGTH_SHORT).show();
    }

    public void stopService(View view) {
        Intent intent = new Intent(this, ImageService.class);
        stopService(intent);
        Toast.makeText(getApplicationContext(), "Service Stopped.",
                Toast.LENGTH_SHORT).show();
    }

}
