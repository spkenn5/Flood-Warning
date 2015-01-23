package com.sig.flood_warning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.sig.flood_warning.GoogleDirection.OnDirectionResponseListener;

public class MainActivity extends Activity {

  private static final String TAG = MainActivity.class.getSimpleName();

  private GoogleMap map;
  private postTask taskPost;
  private loadFlood floodPost;

  List<ParseGeoPoint> GeoPoint;
  List<ParseObject> GeoObject;

  GoogleDirection gd;
  Document mDoc;
  GPSTracker gps;

  Button buttonAnimate, buttonRequest;

  double latitude = 0.0;
  double longitude = 0.0;

  double gotoLat = 0.0;
  double gotoLong = 0.0;

  boolean firstRun = true;

  EditText etTextField = null;
  String description = "";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    gps = new GPSTracker(this);

    etTextField = (EditText) findViewById(R.id.etSearchField);
    map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

    if (firstRun == true) {
      latitude = gps.getLatitude();
      longitude = gps.getLongitude();
      Marker myLocation = map.addMarker(new MarkerOptions()
          .position(new LatLng(latitude, longitude)).title("You are here")
          .icon(BitmapDescriptorFactory.fromResource(R.drawable.me)));

      // Move the camera instantly to hamburg with a zoom of 15.
      map.moveCamera(CameraUpdateFactory.newLatLngZoom(
          new LatLng(gps.getLatitude(), gps.getLongitude()), 15));

      // Zoom in, animating the camera.
      map.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);
    }
    floodPost = new loadFlood();
    floodPost.execute();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.action_refresh) {
      refreshMap();
      return true;
    } else if (id == R.id.action_report) {
      Toast.makeText(getBaseContext(), "Reporting new flooded area...", Toast.LENGTH_LONG).show();
      floodDesc();
      return true;
    } else if (id == R.id.action_search) {
      Toast.makeText(getBaseContext(), "Searching for " + etTextField.getText(), Toast.LENGTH_LONG)
          .show();
      getSearch();
      return true;
    } else if (id == R.id.action_exit) {
      Toast.makeText(getBaseContext(), "Thank you for using this app", Toast.LENGTH_LONG).show();
      this.finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public void refreshMap() {
    Toast.makeText(getBaseContext(), "Getting new coordinates...", Toast.LENGTH_LONG).show();
    longitude = gps.getLongitude();
    latitude = gps.getLatitude();
    map.clear();
    Marker myLocation = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude))
        .title("You are here").icon(BitmapDescriptorFactory.fromResource(R.drawable.me)));

    // Move the camera instantly to hamburg with a zoom of 15.
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(
        new LatLng(gps.getLatitude(), gps.getLongitude()), 15));

    // Zoom in, animating the camera.
    map.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);

    Log.v(TAG, "Loading flood data...");
    floodPost = new loadFlood();
    floodPost.execute();
  }

  public void addFloodMarker() {
    Marker myFlood;
    GeoPoint = new ArrayList<ParseGeoPoint>();
    Log.v(TAG, "Adding Flood Marker");
    for (int i = 0; i < GeoObject.size(); i++) {
      Log.v(TAG, "Here ->" + GeoObject.get(i).getParseGeoPoint("Coordinate").toString());
      GeoPoint.add(i, GeoObject.get(i).getParseGeoPoint("Coordinate"));
    }

    for (int i = 0; i < GeoPoint.size(); i++) {
      myFlood = map.addMarker(new MarkerOptions()
          .position(new LatLng(GeoPoint.get(i).getLatitude(), GeoPoint.get(i).getLongitude()))
          .title(GeoObject.get(i).getString("Description"))
          .snippet(GeoObject.get(i).getCreatedAt().toGMTString())
          .icon(BitmapDescriptorFactory.fromResource(R.drawable.warning)));
    }
  }

  public void floodDesc() {
    AlertDialog.Builder alert = new AlertDialog.Builder(this);

    alert.setTitle("Flood Description");
    alert.setIcon(getResources().getDrawable(R.drawable.really));
    alert.setMessage("Seberapa parah sih banjirnya? ");

    // Set an EditText view to get user input
    final EditText input = new EditText(this);
    alert.setView(input);

    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        reportFlood(input.getText().toString());
        // Do something with value!
      }
    });

    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        // Canceled.
      }
    });

    alert.show();
  }

  public void reportFlood(String edit) {
    map.clear();
    Marker myLocation = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude))
        .title("Road is Flooded").icon(BitmapDescriptorFactory.fromResource(R.drawable.warning)));
    description = edit;
    Toast.makeText(getBaseContext(), edit, Toast.LENGTH_LONG).show();
    taskPost = new postTask();
    taskPost.execute();
  }

  public void postFlood(double tempLangitude, double tempLongitude, String tempDescription) {
    ParseObject flood = new ParseObject("Flood");
    ParseGeoPoint point = new ParseGeoPoint(tempLangitude, tempLongitude);
    flood.put("Coordinate", point);
    flood.put("Description", tempDescription);
    flood.saveInBackground();
  }

  public void getDirection() {
    final LatLng fromPosition = new LatLng(latitude, longitude);
    final LatLng toPosition = new LatLng(gotoLat, gotoLong);
    map.clear();
    gd = new GoogleDirection(this);

    gd.setOnDirectionResponseListener(new OnDirectionResponseListener() {
      public void onResponse(String status, Document doc, GoogleDirection gd) {
        mDoc = doc;
        map.addPolyline(gd.getPolyline(doc, 3, Color.RED));
        map.addMarker(new MarkerOptions().position(fromPosition).icon(
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        map.addMarker(new MarkerOptions().position(toPosition).icon(
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
      }
    });

    gd.setLogging(true);
    gd.request(fromPosition, toPosition, GoogleDirection.MODE_WALKING);
  }

  public void getSearch() {
    Geocoder geocoder = new Geocoder(this);
    List<Address> addresses = null;
    try {
      addresses = geocoder.getFromLocationName(etTextField.getText().toString(), 1);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    if (addresses != null) {
      if (addresses.size() > 0) {
        gotoLat = addresses.get(0).getLatitude();
        gotoLong = addresses.get(0).getLongitude();
        Toast.makeText(getBaseContext(),
            etTextField.getText() + "'s Coordinates are Lat: " + gotoLat + " & Long: " + gotoLong,
            Toast.LENGTH_LONG).show();
      }
    } else {
      Toast.makeText(getBaseContext(), "Area could not be found...", Toast.LENGTH_LONG).show();
    }
    getDirection();
  }

  class postTask extends AsyncTask<Void, Void, Void> {

    @Override
    protected Void doInBackground(Void... params) {
      // TODO Auto-generated method stub
      postFlood(latitude, longitude, description);
      return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(Void result) {
      // TODO Auto-generated method stub
      super.onPostExecute(result);
      refreshMap();
    }

  }

  class loadFlood extends AsyncTask<Void, Void, List<ParseObject>> {

    @Override
    protected List<ParseObject> doInBackground(Void... params) {
      // TODO Auto-generated method stub
      List<ParseObject> result = null;
      try {
        Log.v(TAG, "Query flood data");
        result = getFlood();
        Log.v(TAG, "Found  = " + result.size() + " Spots");
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(List<ParseObject> result) {
      // TODO Auto-generated method stub
      super.onPostExecute(result);
      Log.v(TAG, "Size = " + result.size());
      GeoObject = new ArrayList<ParseObject>();
      GeoObject.addAll(result);
      Log.v(TAG, "Add flood marker");
      addFloodMarker();
    }
  }

  public static List<ParseObject> getFlood() throws ParseException {
    // posting by self
    List<ParseObject> result = null;
    ParseQuery<ParseObject> query = ParseQuery.getQuery("Flood");
    query.setLimit(100);
    result = query.find();
    return result;
  }
}
