
package com.location.geofencecreator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/*
Referenced Docs from https://developer.android.com/training/location/geofencing.html
 PART 1 CREATE AND ADD GEOFENCES (1ST-4TH) inside MainActivity
 PART 2 HANDLE GEOFENCE TRANSITIONS inside GeofencetransitionsIntentService and SEND NOTIFICATION

 Note: MainActivity class also contains 2 inner classes towards the bottom of the page:
        Constants class (This is where we set the RADIUS and EXPIRATION time for our Geofence
        GeofenceCreationDialog class (This is the functionality that supports an AlertDialog to enter the Geofence name and message)
 */

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status>, LocationListener {

    ///////////////////////////////////////////////////////FIELDS//////////////////////////////////////////
    protected ArrayList<Geofence> mGeofenceList;
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;

    public static final String EXTRA_GEOFENCE_MESSAGE = "Extra_Geofence_Message";

    //declaring latitude and longitude so these can be retrieved dynamically
    private double mLatitude;
    private double mLongitude;

    static private String mGeofenceName = "";
    static private String mGeofenceMessage = "";

    static View inflatedView;//inflated view for dialog_fragment
    GeofenceCreationDialog mDialog;

    Constants constants;
    ArrayList<String> mDisplayList;
    ListView mListView;

    ///////////////////////////////////////////////////////METHODS//////////////////////////////////////////

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("KEY_LIST", mDisplayList);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = (ListView) findViewById(R.id.listView);


        if (savedInstanceState != null) {
            mDisplayList = savedInstanceState.getStringArrayList("KEY_LIST");
        }else{
            mDisplayList = new ArrayList<>();
        }
        mListView.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,  mDisplayList));

        constants = new Constants();

        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<Geofence>();


        // To initialize and  build GoogleApiClient whihc is needed for location services and also for adding geofences
        initGoogleApiClient();


        inflatedView = LayoutInflater.from(this).inflate(R.layout.dialog_fragment, null);//inflated for the dialog
    }

    public void populateGeofenceList() {



        constants.LANDMARKS.put(mGeofenceName, new LatLng(mLatitude, mLongitude));

        for (Map.Entry<String, LatLng> entry : constants.LANDMARKS.entrySet()) {

            //1st CREATE GEOFENCE OBJECTS
            /*
            First, use Geofence.Builder to create a geofence,
            setting the desired radius, duration, and transition types for the geofence.
            For example, to populate a list object named mGeofenceList:
             */
            mGeofenceList.add(new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(entry.getKey())

                    // Set the circular region of this geofence.
                    .setCircularRegion(
                            entry.getValue().latitude,
                            entry.getValue().longitude,
                            Constants.GEOFENCE_RADIUS_IN_METERS
                    )

                    // Set the expiration duration of the geofence. This geofence gets automatically
                    // removed after this period of time.
                    .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECS)

                    // Set the transition types of interest. Alerts are only generated for these
                    // transition. We track entry and exit transitions in this sample.
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)

                    // Create the geofence.
                    .build());
        }
    }


    public void onResult(Status status) {
        if (status.isSuccess()) {
            Toast.makeText(
                    this,
                    "Geofences Added",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorMessage(this,
                    status.getStatusCode());
        }
    }

    //3RD DEFINE AN INTENT for GEOFENCE TRANSITIONS-- ie setup and return the PendingIntent to get the service
/*
 An IntentService can post a notification, do long-running background work,
  send intents to other services, or send a broadcast intent.
  The following encapsulates a PendingIntent that starts an IntentService:
 */
    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        intent.putExtra(EXTRA_GEOFENCE_MESSAGE, mGeofenceMessage);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling addGeoFences()
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //4th ADD GEOFENCES
    /*
    To add geofences, use the GeoencingApi.addGeofences() method. Provide the Google API client, the GeofencingRequest object, and the PendingIntent.
    The following snippet, which processes the results in onResult(), assumes that the main activity implements ResultCallback:
     */
    public void onAddGeofencesBtnHandler(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

      //  Toast.makeText(MainActivity.this, "Geofence is being activated for the locations in the list for the GeofencingRequest", Toast.LENGTH_SHORT).show();
        launchCreateGeofenceDialog();

    }

    private void launchCreateGeofenceDialog() {
        //launch the Dialog to Customize and Create the Geofence
        mDialog = GeofenceCreationDialog.createGeofenceCreationDialog();
        mDialog.show(getFragmentManager(), "DIALOG_TAG");
    }

    private void addGeofences() {
        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
        }
    }


    /**
     * Builds a GoogleApiClient and uses the addApi} method to request the LocationServices API.
     */
    protected synchronized void initGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        //Create a locationRequest to requestLocationUpdates
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(120000);//updates every 2 minutes
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Handle error if client fails to connect here
        Toast.makeText(MainActivity.this, "Error Connection Failed\n" + result.getErrorCode(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        //If your client was connected then it broke (ie suspended) simply reconnect
        mGoogleApiClient.connect();
    }

    //2nd SPECIFY GEOFENCES and INITIAL TRIGGERS
    /*
    The following snippet uses the GeofencingRequest class and its nested GeofencingRequestBuilder class
    to specify the geofences to monitor and to set how related geofence events are triggered:
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }


    /*
       this callback method is triggered everytime requestLocationUpdates() is called to update the location
     */
    @Override
    public void onLocationChanged(Location location) {
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        Toast.makeText(MainActivity.this, "Latitude:" + mLatitude + "\nLongitude:" + mLongitude, Toast.LENGTH_SHORT).show();
    }

    /*
      This method is triggered when the Create Geofence dialog button is clicked
     */
    public void createGeofenceDialogBtnHandler(View view) {

        EditText etName = (EditText) inflatedView.findViewById(R.id.etGeofenceName);
        EditText etMessage = (EditText) inflatedView.findViewById(R.id.etGeofenceMessage);
        mGeofenceName = etName.getText().toString();
        mGeofenceMessage = etMessage.getText().toString();

        mDialog.getDialog().dismiss();
        showGeofenceDetailsDialog();

    }

    /*
    Method to display the details of the newly created Geofence
     */
    private void showGeofenceDetailsDialog() {
        AlertDialog detailDialog = new AlertDialog.Builder(this)
                .setTitle("Details of Your Geofence")
                .setMessage("Location Name: " + mGeofenceName +
                        "\nNotification Message: " + mGeofenceMessage +
                        "\n\nLatitude: " + mLatitude +
                        "\nLongitude: " + mLongitude)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();// OR dialog.cancel();
                        populateGeofenceList();
                        addGeofences();

                        mDisplayList.add(mGeofenceName);
                    }
                })
                .create();
        detailDialog.show();
    }

    public final class Constants {// final class means that this class cannot be extended (or inherited)

        private Constants() {
        }


        public static final float GEOFENCE_RADIUS_IN_METERS = 50; //1 meter is 3.28084, 50 meters is 164.042 feet

        /**
         * Used to set an expiration time for a geofence. After this amount of time, the geofence expires ie.Location Services stops tracking the geofence
         */
        public static final long GEOFENCE_EXPIRATION_IN_MILLISECS =
                3*1200000; //3*20 minutes expiration = 1hr

        /**
         * Map for storing information about home locations in current location.
         */
        public final HashMap<String, LatLng> LANDMARKS = new HashMap<String, LatLng>();

    }

    /*
     class that makes use of our dialog_fragment to display the pop up ie Alertdialog to enable the user to enter the Geofence attributes (name and message)
     */
    static public class GeofenceCreationDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {


            AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                    .setTitle("Customize and Create your Geofence")
                    .setView(inflatedView)
                    .create();


            return alertDialog;
        }

        public static GeofenceCreationDialog createGeofenceCreationDialog() {
            return new GeofenceCreationDialog();
        }

        /*
        to handle error, illegalstateexception-the-specified-child-already-has-a-parent
         */
        @Override
        public void onDestroyView() {
            super.onDestroyView();
            if (inflatedView != null) {
                ViewGroup parentViewGroup = (ViewGroup) inflatedView.getParent();
                if (parentViewGroup != null) {
                    parentViewGroup.removeAllViews();
                }
            }
        }

    }

}
