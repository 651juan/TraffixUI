package traffix.jaba.com.traffix;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class TrafficMapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    static String  TRAFFIC = "TRAFFIC";
    static String  LOCATION = "LOCATION";
    static String  SPEED = "SPEED";
    static String  COUNT = "COUNT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traffic_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                LinearLayout info = new LinearLayout(getApplicationContext());
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(getApplicationContext());
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(getApplicationContext());
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });

        mMap.setOnMapClickListener(this);
        setMarkers();
    }

    private void setMarkers() {
        Log.i("in Markers", "now");
        LatLng Attard = new LatLng(35.8931352,14.4513628);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Attard, 10));
        String rawData = getIntent().getStringExtra("DATA");
        if (rawData != null) {
            try {
                JSONObject serverRes = new JSONObject(rawData);
                JSONObject a = getLatestFromJson(serverRes);
                if (a.get(TRAFFIC).equals("true")) {
                    mMap.addMarker(new MarkerOptions().position(Attard).title(a.get(LOCATION) + " is currently congested with " + a.get(COUNT) + " vehicles.\n Our traffic score is: " + a.get(SPEED)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                } else {
                    mMap.addMarker(new MarkerOptions().position(Attard).title(a.get(LOCATION) + " is currently not congested with " + a.get(COUNT) + " vehicles.\n Our traffic score is: " + a.get(SPEED)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMapClick(LatLng point) {
        new RetrieveFeedTask(getApplicationContext()).execute();
        setMarkers();
    }

    private class RetrieveFeedTask extends AsyncTask<Void, Void, String> {

        Context context;

        private RetrieveFeedTask(Context context) {
            this.context = context;
        }

        protected String doInBackground(Void... urls) {
            Log.i("in Background", "now");
            try {
                File saveto = File.createTempFile("result", null, getApplicationContext().getCacheDir());
                if (!saveto.exists()) {
                    try {
                        saveto.createNewFile();
                    } catch (IOException e) {
                        Log.e(e.getMessage(), e.toString());
                    }
                }
                downloadAndSaveFile("", 21, "", "", "", saveto);
                return printfile(saveto);
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);
            }
            return null;
        }

        protected void onPostExecute(String response) {
            getIntent().putExtra("DATA", response);
        }


        private Boolean downloadAndSaveFile(String server, int portNumber, String user, String password, String filename, File localFile) throws IOException {
            FTPClient ftp = null;

            try {
                ftp = new FTPClient();
                ftp.connect(server, portNumber);

                ftp.login(user, password);
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                ftp.enterLocalPassiveMode();

                OutputStream outputStream = null;
                boolean success = false;
                try {
                    outputStream = new BufferedOutputStream(new FileOutputStream(localFile));
                    success = ftp.retrieveFile(filename, outputStream);
                } catch (Exception e) {
                    Log.i(e.getMessage(), e.toString());
                } finally {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                }

                return success;
            } catch (Exception e) {
                Log.e("IOFail", e.getMessage());
            } finally {
                if (ftp != null) {
                    ftp.logout();
                    ftp.disconnect();
                }
            }
            return true;
        }

        private String printfile(File f) {
            StringBuilder sb = new StringBuilder();
            try {
                FileInputStream fis = new FileInputStream(f);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader bufferedReader = new BufferedReader(isr);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line + '\n');
                }

            } catch (IOException e) {
                Log.e(e.toString(), e.getMessage());
            }
            return sb.toString();
        }
    }

    public JSONObject getLatestFromJson(JSONObject input) {
        Iterator<?> keys = input.keys();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US);
        try {
            Date latest = sdf.parse("01-Jan-1900 00:00:00");
            String latestKey = "";
            while( keys.hasNext() ) {
                String key = (String) keys.next();
                Date  current = sdf.parse(key);
                if (current.after(latest)) {
                    latest = current;
                    latestKey = key;
                }
            }
            return (JSONObject) input.getJSONArray(latestKey).get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
