package com.example.accele;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    boolean timer = false;
    Button start;
    TextView timer3;
    TextInputEditText name,motion;
    Handler handler;
    long tmill,tstart,tbuff,tupdate;
    int mil,sec,min;
    private SensorManager sensorManager;
    Sensor accelerometer;
    double ax,ay,az;
    private FileOutputStream outputStream;
    private OutputStreamWriter writer;
    private File file;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent serviceIntent = new Intent(this, forground.class);
        startForegroundService(serviceIntent);

        name = (TextInputEditText) findViewById(R.id.Name);
        timer3 = (TextView) findViewById(R.id.timer3);
        motion = (TextInputEditText) findViewById(R.id.motion);
        start = (Button) findViewById(R.id.start);
        handler = new Handler();

    }

    public boolean foregroundServiceRunning(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service: activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if(forground.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public Runnable runnable = new Runnable() {
        @Override
        public void run() {
            tmill = SystemClock.uptimeMillis() - tstart;
            tupdate = tbuff + tmill;
            sec = (int) (tupdate/1000);
            min = sec / 60;
            sec = sec % 60;
            mil = (int) (tupdate%100);
            timer3.setText(String.format("%02d",min) + ":" + String.format("%02d",sec) + ":" + String.format("%02d",mil));
            handler.postDelayed(this,0);
        }
    };

    public void starttimere(View view) throws IOException {
        boolean checked = TextUtils.isEmpty(name.getText().toString())||TextUtils.isEmpty(motion.getText().toString());
        if (checked) {
            Toast.makeText(this, "this field is required", Toast.LENGTH_LONG).show();
        }
        else{
            if (timer == false) {
                sensor();
                timer = true;
                start.setText("STOP");
                name.setFocusable(false);
                motion.setFocusable(false);
                start.setTextColor(ContextCompat.getColor(this, R.color.white));
                start.setBackgroundColor(ContextCompat.getColor(this, R.color.black));
                tstart = SystemClock.uptimeMillis();
                handler.postDelayed(runnable, 0);
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                file = new File(downloadsDir, motion.getText().toString()+".csv");
                try {
                    outputStream = new FileOutputStream(file);
                    writer = new OutputStreamWriter(outputStream);
                    Toast.makeText(this, "file created successfully", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                timer = false;
                start.setText("START");
                stopRecording();
                handler.removeCallbacks(runnable);
                tbuff += tmill;
                timer3.setText("00:00:00");
                tbuff = 0L;
                tstart = 0L;
                tupdate = 0L;
                tmill = 0L;
                min = 0;
                sec = 0;
                mil = 0;
                start.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
                start.setTextColor(ContextCompat.getColor(this, R.color.black));
                name.setFocusable(true);
                motion.setFocusable(true);
            }
        }
    }

    private void sensor() {
        sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (sensorEvent.sensor.getType()==Sensor.TYPE_ACCELEROMETER  && timer){
                    ax = sensorEvent.values[0];
                    ay = sensorEvent.values[1];
                    az = sensorEvent.values[2];
                    try {
                        writer.write(String.format(Locale.US, "%.10f,%.10f,%.10f\n", ax, ay, az));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        sensorManager.registerListener(sensorEventListener,accelerometer,SensorManager.SENSOR_DELAY_GAME);
    }

    public void stopRecording() {
        try {
            writer.close();
            outputStream.close();
            Toast.makeText(this, "Data saved successfully", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}