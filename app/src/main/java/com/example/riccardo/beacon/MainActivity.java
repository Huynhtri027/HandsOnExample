package com.example.riccardo.beacon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;

import com.example.riccardo.beacon.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final static String my_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
    private final static String TAG = "HttpPost";
    private final static String TAG2 = "MainActivity";
    private final static String URL = "http://smartsupermarket.altervista.org/provaRead.php";
    private BeaconManager beaconManager; //it is the gateway to beacon's interactions
    private Region region;
    private String scanId;
    private RequestQueue queue;
    private String uuid;
    private int major;
    private int minor;
    private Beacon lastSeenBeacon;

    /* The map is instantiated statically
       blueberry is on the writing desk and the mint on the shoe rack.
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beaconManager = new BeaconManager(getApplicationContext());
        beaconManager.setForegroundScanPeriod(5000, 5000);

        region = new Region("monitored region", UUID.fromString(my_UUID), null, null);
        //start monitoring the blueberry beacon
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startMonitoring(region);
            }
        });

        queue = Volley.newRequestQueue(this);


        //ranging listener for obtain the nearest beacon and show the values of the map
        //associated with it.

        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> list) {
                if (!list.isEmpty()) {
                    Beacon nearestBeacon = list.get(0);
                    if (nearestBeacon != lastSeenBeacon) {
                        lastSeenBeacon = nearestBeacon;
                        uuid = nearestBeacon.getProximityUUID().toString().replace("-", "");
                        major = nearestBeacon.getMajor();
                        minor = nearestBeacon.getMinor();
                        StringRequest request = setupHttpPost();
                        queue.add(request);
                        Log.d(TAG2, "UUID = " + uuid + ", Major = " + major + ", Minor = " + minor);
                    }
                }
            }
        });
    }


    private StringRequest setupHttpPost(){
        StringRequest request = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Response = " + response);
                ListView listView = (ListView) findViewById(R.id.listview);
                String[] split = response.split("<br>");
                Item item;
                ArrayList<Item> items = new ArrayList();
                for (String s : split){
                    Log.d(TAG, s);
                    String[] fields = s.split(",");
                    if (fields.length == 3) {
                        Log.d(TAG, "f1 " + fields[0] + " f2 " + fields[1] + " f3 " + fields[2]);
                        item = new Item(fields[0], fields[1], fields[2]);
                        items.add(item);
                    }
                }
                CustomAdapter adapter = new CustomAdapter(getApplicationContext(), items);
                listView.setAdapter(adapter);
/*
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.row, split);
                listView.setAdapter(arrayAdapter);*/

            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error) {
                // error
                Log.e(TAG, "Error " + error.toString());
            }
        })
        {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("UUID", uuid);
                params.put("major", "" + major);
                params.put("minor", "" + minor);
                return params;
            }
        };
        return request;
    }

    @Override
    protected void onResume() {
        super.onResume();
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startRanging(region);
            }
        });
    }

    @Override
    protected void onPause() {
        beaconManager.stopRanging(region);

        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                scanId = beaconManager.startTelemetryDiscovery();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        beaconManager.stopTelemetryDiscovery(scanId);
    }

}