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
import com.estimote.sdk.repackaged.retrofit_v1_9_0.retrofit.http.POST;
import com.estimote.sdk.telemetry.EstimoteTelemetry;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.net.*;

public class MainActivity extends AppCompatActivity {
    private final static String blueberryUUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
    private final static int blueberryMajor = 23082;
    private final static int blueberryMinor = 20505;
    private final static String mintUUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
    private final static int mintMajor = 30781;
    private final static int mintMinor = 5475;
    private final static String TAG = "HttpPost";
    private final static String TAG2 = "MainActivity";

    private final static String URL = "http://smartsupermarket.altervista.org/provaRead.php";
    private BeaconManager beaconManager; //it is the gateway to beacon's interactions
    private Region region;
    private static final Map<String, List<String>> PLACES_BY_BEACONS;
    private String scanId;
    private RequestQueue queue;
    private String uuid;
    private int major;
    private int minor;
    private Beacon lastSeenBeacon;

    /* The map is instantiated statically
       blueberry is on the writing desk and the mint on the shoe rack.
     */


    static {
        Map<String, List<String>> placesByBeacons = new HashMap<>();
        placesByBeacons.put(blueberryMajor + ":" + blueberryMinor, new ArrayList<String>() {{
            add("matita");
            // read as: "Heavenly Sandwiches" is closest
            // to the beacon with major 22504 and minor 48827
            add("Penna");
            // "Green & Green Salads" is the next closest
            add("gomma");
            // "Mini Panini" is the furthest away
        }});
        placesByBeacons.put(mintMajor + ":" + mintMinor, new ArrayList<String>() {{
            add("scarpe");
            add("lucidascarpe");
            add("scarpiera");
        }});
        PLACES_BY_BEACONS = Collections.unmodifiableMap(placesByBeacons);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beaconManager = new BeaconManager(getApplicationContext());

        region = new Region("ranged region", UUID.fromString(blueberryUUID), null, null);

        //start monitoring the blueberry beacon
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startMonitoring(new Region(
                        "monitored region",
                        UUID.fromString(blueberryUUID),
                        blueberryMajor, blueberryMinor));
            }
        });

        queue = Volley.newRequestQueue(this);
        /*
        System.out.println("LOCATION = " + beaconManager.startLocationDiscovery());
        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<Beacon> list) {
                showNotification("CI SIAMO", "BLUEBERRY BEACON HAS BEEN FOUND");
            }
            @Override
            public void onExitedRegion(Region region) {
                // could add an "exit" notification too if you want (-:
            }
        });*/

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
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.row, split);
                listView.setAdapter(arrayAdapter);
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

    public void showNotification(String title, String message) {
        Intent notifyIntent = new Intent(this, MainActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivities(this, 0,
                new Intent[]{notifyIntent}, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();
        notification.defaults |= Notification.DEFAULT_SOUND;
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }

    private List<String> placesNearBeacon(Beacon beacon) {
        String beaconKey = String.format("%d:%d", beacon.getMajor(), beacon.getMinor());
        if (PLACES_BY_BEACONS.containsKey(beaconKey)) {
            return PLACES_BY_BEACONS.get(beaconKey);
        }
        return Collections.emptyList();
    }
}