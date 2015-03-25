package com.oakraw.gpstracking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;


public class MainActivity extends Activity implements View.OnClickListener {

    LocationManager myLocationManager;
    Button btn_start;
    Button btn_stop;
    TextView ime;
    String latt;
    String lonn;
    String speed = "0";
    //final String SERVER_IP = "192.168.42.130";
    String SERVER_IP = "";
    MyThread thread = null;
    private TextView latTextView;
    private TextView lngTextView;

    private LocationListener mLocationListener;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;
    private MyLocationListener myLocationListener;
    private ToggleButton tram3;
    private ToggleButton tram2;
    private ToggleButton tram1;

    int selected = 0;
    private ProgressDialog progressDialog;

    public void initWidget() {
        btn_start = (Button) findViewById(R.id.btn_start);
        btn_start.setOnClickListener(this);

        btn_stop = (Button) findViewById(R.id.btn_stop);
        btn_stop.setOnClickListener(this);

        //ime = (TextView)findViewById(R.id.ime);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initWidget();
        myLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        myLocationListener = new MyLocationListener();
        myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES, myLocationListener);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Finding location...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        if (!this.isGpsEnable()) {
            Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            startActivity(intent);
        }

        latTextView = (TextView) findViewById(R.id.lat);
        lngTextView = (TextView) findViewById(R.id.lng);

        latTextView.setText("lat: null");
        lngTextView.setText("lng: null");

        tram1 = (ToggleButton) findViewById(R.id.tram1);
        tram2 = (ToggleButton) findViewById(R.id.tram2);
        tram3 = (ToggleButton) findViewById(R.id.tram3);

        tram1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    resetButton();
                    tram1.setChecked(true);
                    selected = 1;
                } else {
                    selected = 0;
                    stopTracking();
                }
            }
        });
        tram2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    resetButton();
                    tram2.setChecked(true);
                    selected = 2;
                } else {
                    selected = 0;
                    stopTracking();
                }
            }
        });
        tram3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    resetButton();
                    tram3.setChecked(true);
                    selected = 3;

                } else {
                    selected = 0;
                    stopTracking();
                }
            }
        });

    }//////////////////////// end onCreate /////////////////////

    private void resetButton() {
        tram1.setChecked(false);
        tram2.setChecked(false);
        tram3.setChecked(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start:
                if (selected != 0) {
                    thread = new MyThread();
                    ////  ดึงค่าจาก GPS


                    if (!thread.isAlive()) {
                        Toast.makeText(this, "Tracker Started..", Toast.LENGTH_SHORT).show();
                        thread.start();
                    }
                }else{
                    Toast.makeText(this, "Please select tram no.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_stop:
                stopTracking();
                break;
        }
    }

    private void stopTracking(){
        if (thread != null) {
            thread.finish = true;
            Toast.makeText(this, "Tracker Stopped..", Toast.LENGTH_SHORT).show();
        }
    }
    //ดึงค่าเลข  IMEI ของเครื่อง
    public String getIme() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String device_id = tm.getDeviceId();
        //return device_id;
        return "1111111111";
    }

    // รับ GPS data
    public String getGpsData() {
        return latt + "," + lonn + "," + speed;
    }

    // ส่งข้อมูลไปที่ SERVER ด้วย UDP
    public void sendUDPMessage(int t) throws java.io.IOException {
        Calendar c = Calendar.getInstance();
        int sec = c.get(Calendar.SECOND);
        int min = c.get(Calendar.MINUTE);
        int hour = c.get(Calendar.HOUR);
        String times = hour + ":" + min + ":" + sec;

        try {
            HttpClient httpclient = new DefaultHttpClient();

            HttpGet request = new HttpGet();
            URI website = new URI("http://192.168.1.102:8888/location_update.php?time='" + times + "'&lat=" + latt + "&lng=" + lonn + "&speed=" + speed + "&tram=" + selected);
            request.setURI(website);
            HttpResponse response = httpclient.execute(request);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent()));
            Log.d("log_tag", website.toString());


            // NEW CODE
            String line = in.readLine();

        } catch (Exception e) {
            Log.e("log_tag", "Error in http connection " + e.toString());
        }
    }

    public boolean isGpsEnable() { // เมธอดตรวจสอบว่าเปิด GPS หรือไม่
        boolean isgpsenable = false;
        String provider = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (!provider.equals("")) { //GPS is Enabled
            isgpsenable = true;
        }
        return isgpsenable;
    }

    private class MyThread extends Thread {
        private static final int DELAY = 10000;      //delay  3 วินาที
        private boolean finish = false;
        int i = 0;

        public void run() {
            while (true) {
                i++;
                try {
                    sendUDPMessage(i);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    sleep(DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (finish) {
                    return;
                }
            }
        }
    } ////////// end  Thread Class ////////////////////////

    private class MyLocationListener implements LocationListener {
        public void onLocationChanged(Location argLocation) {
            // TODO Auto-generated method stub
            latt = String.valueOf(argLocation.getLatitude());
            lonn = String.valueOf(argLocation.getLongitude());
            float sp = argLocation.getSpeed();
            speed = String.valueOf(sp);
            latTextView.setText("lat: " + latt);
            lngTextView.setText("lng: " + lonn);
            progressDialog.hide();
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }  ///  end Location class


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (thread != null) {
            thread.finish = true;
            Toast.makeText(this, "Tracker Stopped..", Toast.LENGTH_SHORT).show();
        }
        myLocationManager.removeUpdates(myLocationListener);


    }
}
