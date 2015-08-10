/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANoY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.lazooz.lbm;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

//import com.example.android.common.activities.SampleActivityBase;
import com.lazooz.lbm.communications.ServerCom;
import com.lazooz.lbm.logger.Log;
import com.lazooz.lbm.logger.LogWrapper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.lazooz.lbm.preference.MySharedPreferences;
import com.lazooz.lbm.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import static com.lazooz.lbm.R.layout.activity_ride_share_request;

public class RideShareEnterRequestActivity extends ActionBarActivity
        implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "lbm RideShareEnterRequestActivity" ;
    /**
     * GoogleApiClient wraps our service connection to Google Play Services and provides access
     * to the user's sign in state as well as the Google's APIs.
     */
    protected GoogleApiClient mGoogleApiClient;

    private PlaceAutocompleteAdapter mAdapter;

    private AutoCompleteTextView mAutocompleteViewDest;

    private TextView mPlaceDetailsText;

    private static String DestPlaceId = null;

    private static  LatLngBounds bounds = new LatLngBounds(
            new LatLng(-34.041458, 150.790100), new LatLng(-33.682247, 151.383362));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        // Wraps Android's native log framework
        LogWrapper logWrapper = new LogWrapper();
        Log.setLogNode(logWrapper);


        if (!MySharedPreferences.getInstance().getUserProfile(this,"SET").equalsIgnoreCase("DONE"))
        {
            showDialogForProfile(this);
        }

        // Set up the Google API Client if it has not been initialised yet.
        if (mGoogleApiClient == null) {
            rebuildGoogleApiClient();
        }

        setContentView(activity_ride_share_request);

        // Retrieve the AutoCompleteTextView that will display Place suggestions.
        mAutocompleteViewDest = (AutoCompleteTextView)
                findViewById(R.id.autocomplete_places_dest);

        // Register a listener that receives callbacks when a suggestion has been selected
        mAutocompleteViewDest.setOnItemClickListener(mAutocompleteDestClickListener);

        // Retrieve the TextView that will display details of the selected place.
        mPlaceDetailsText = (TextView) findViewById(R.id.place_details);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        if (provider == null)
        {
            Utils.messageToUser(this, "RideShare", "Your location service is off.Please turn it on",RideShareEnterRequestActivity.this);
            return;
        }
        Location location = locationManager.getLastKnownLocation(provider);

        if (location!=null) {
            bounds = new LatLngBounds.Builder()
                    .include(new LatLng(location.getLatitude(), location.getLongitude()))
                    .build();
        }
        else
        {
            Log.v("location == null","null");
        }

        // Set up the adapter that will retrieve suggestions from the Places Geo Data API that cover
        // the entire world.
        mAdapter = new PlaceAutocompleteAdapter(this, android.R.layout.simple_list_item_1,
                bounds, null);

        mAutocompleteViewDest.setAdapter(mAdapter);



        final Button FindMatchButton = (Button) findViewById(R.id.find_match_submit);
        FindMatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FindMatchButton.setEnabled(false);
                if (DestPlaceId == null) {
                    Toast.makeText(getApplicationContext(), "Please enter your destination", Toast.LENGTH_SHORT).show();
                } else {
                    SubmitMatchRequestToServer(null,null,null,null,null,DestPlaceId,null,null,"barcelona");
                }
            }
        });
    }

    /**
     * Listener that handles selections from suggestions from the AutoCompleteTextView that
     * displays Place suggestions.
     * Gets the place id of the selected item and issues a request to the Places Geo Data API
     * to retrieve more details about the place.
     *
     * @see com.google.android.gms.location.places.GeoDataApi#getPlaceById(com.google.android.gms.common.api.GoogleApiClient,
     * String...)
     * @param RideShareEnterRequestActivity
     */
    private boolean showDialogForProfile(final RideShareEnterRequestActivity RideShareEnterRequestActivity)
    {
        try {
            AlertDialog ad = new AlertDialog.Builder(this).create();
            ad.setTitle("Personal Profile Missing");
            ad.setMessage("Please fill up personal profile");
            ad.setButton(DialogInterface.BUTTON_POSITIVE, this.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    Intent intent;
                    intent = new Intent(RideShareEnterRequestActivity, ProfileGoogleActivity.class);
                    startActivity(intent);

                }
            });
            ad.setButton(DialogInterface.BUTTON_NEGATIVE,"later", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    finish();
                }
            });
            ad.setCanceledOnTouchOutside(false);
            ad.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  true;
    }

    /**
     * Listener that handles selections from suggestions from the AutoCompleteTextView that
     * displays Place suggestions.
     * Gets the place id of the selected item and issues a request to the Places Geo Data API
     * to retrieve more details about the place.
     *
     * @see com.google.android.gms.location.places.GeoDataApi#getPlaceById(com.google.android.gms.common.api.GoogleApiClient,
     * String...)
     */
    private AdapterView.OnItemClickListener mAutocompleteDestClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            /*
             Retrieve the place ID of the selected item from the Adapter.
             The adapter stores each Place suggestion in a PlaceAutocomplete object from which we
             read the place ID.
              */
            final PlaceAutocompleteAdapter.PlaceAutocomplete item = mAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);
            Log.i(TAG, "Autocomplete item selected: " + item.description);

            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
              details about the place.
              */
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);

            Toast.makeText(getApplicationContext(), "Clicked: " + item.description,
                    Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Called getPlaceById to get Place details for " + item.placeId);
            DestPlaceId = item.placeId.toString();
        }
    };

    /**
     * Callback for results from a Places Geo Data API query that shows the first place result in
     * the details view on screen.
     */
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                // Request did not complete successfully
                Log.e(TAG, "Place query did not complete. Error: " + places.getStatus().toString());

                return;
            }
            // Get the Place object from the buffer.
            final Place place = places.get(0);

            // Format details of the place for display and show it in a TextView.
            /*
            mPlaceDetailsText.setText(formatPlaceDetails(getResources(), place.getName(),
                    place.getId(), place.getAddress(), place.getPhoneNumber(),
                    place.getWebsiteUri()));
*/
            Log.i(TAG, "Place details received: " + place.getName());
        }
    };

    private static Spanned formatPlaceDetails(Resources res, CharSequence name, String id,
                                              CharSequence address, CharSequence phoneNumber, Uri websiteUri) {
        Log.e(TAG, res.getString(R.string.place_details, name, id, address, phoneNumber,
                websiteUri));
        return Html.fromHtml(res.getString(R.string.place_details, name, id, address, phoneNumber,
                websiteUri));

    }


    /**
     * Construct a GoogleApiClient for the {@link com.google.android.gms.location.places.Places#GEO_DATA_API} using AutoManage
     * functionality.
     * This automatically sets up the API client to handle Activity lifecycle events.
     */
    protected synchronized void rebuildGoogleApiClient() {
        // When we build the GoogleApiClient we specify where connected and connection failed
        // callbacks should be returned, which Google APIs our app uses and which OAuth 2.0
        // scopes our app requests.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0 /* clientId */, this)
                .addConnectionCallbacks(this)
                .addApi(Places.GEO_DATA_API)
                .build();
    }

    /**
     * Called when the Activity could not connect to Google Play services and the auto manager
     * could resolve the error automatically.
     * In this case the API is not available and notify the user.
     *
     * @param connectionResult can be inspected to determine the cause of the failure
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.e(TAG, "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());

        // TODO(Developer): Check error code and notify the user of error state and resolution.
        Toast.makeText(this,
                "Could not connect to Google API Client: Error " + connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();

        // Disable API access in the adapter because the client was not initialised correctly.
        mAdapter.setGoogleApiClient(null);

    }


    @Override
    public void onConnected(Bundle bundle) {
        // Successfully connected to the API client. Pass it to the adapter to enable API access.
        mAdapter.setGoogleApiClient(mGoogleApiClient);
        Log.i(TAG, "GoogleApiClient connected.");

    }

    @Override
    public void onConnectionSuspended(int i) {
        // Connection to the API client has been suspended. Disable API access in the client.
        mAdapter.setGoogleApiClient(null);
        Log.e(TAG, "GoogleApiClient connection suspended.");
    }

    protected void SubmitMatchRequestToServer(String SourceLat,String SourceLong,String SourceId,
                                              String DestLat,String DestLong,String DestId,
                                              String ShareTaxi,String ShareCar,String Sportteam ) {
        SubmitMatchRequestToServer submitMatchRequestToServer = new SubmitMatchRequestToServer();
        submitMatchRequestToServer.execute(SourceLat, SourceLong, SourceId, DestLat, DestLong, DestId, ShareTaxi, ShareCar, Sportteam);

    }

    private class SubmitMatchRequestToServer extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... params) {

            ServerCom bServerCom = new ServerCom(RideShareEnterRequestActivity.this);

            String lSourceLat  = params[0];
            String lSourceLong = params[1];
            String lSourceId   = params[2];
            String lDestLat    = params[3];
            String lDestLong   = params[4];
            String lDestId     = params[5];
            String lShareTaxi  = params[6];
            String lShareCar   = params[7];
            String lSportteam  = params[8];



            JSONObject jsonReturnObj=null;
            try {
                MySharedPreferences msp = MySharedPreferences.getInstance();
                bServerCom.setMatchRequest(msp.getUserId(RideShareEnterRequestActivity.this), msp.getUserSecret(RideShareEnterRequestActivity.this),lSourceLat,lSourceLong,lSourceId,lDestLat,lDestLong,lDestId,lShareTaxi,lShareCar,lSportteam);
                jsonReturnObj = bServerCom.getReturnObject();
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            String serverMessage = "";

            try {
                if (jsonReturnObj == null)
                    serverMessage = "ConnectionError";
                else {
                    serverMessage = jsonReturnObj.getString("message");
                    if (serverMessage.equals("success")){

                    }
                }
            }
            catch (JSONException e) {
                e.printStackTrace();
                serverMessage = "GeneralError";
            }


            return serverMessage;
        }

        @Override
        protected void onPostExecute(String result) {

            if (result.equals("success")){
                Toast.makeText(RideShareEnterRequestActivity.this, "Your request has been sent to the server. Please wait till we found a match", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(RideShareEnterRequestActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                //  MySharedPreferences.getInstance().saveKeyPair(MainZoozActivity.this, "", mScannedKey);
                // UpdateGUI();
            }
            else if (result.equals("credentials_not_valid")){
                Utils.restartApp(RideShareEnterRequestActivity.this);
            }
            else
                Toast.makeText(RideShareEnterRequestActivity.this,"Send request failed.", Toast.LENGTH_LONG).show();
        }


        @Override
        protected void onPreExecute() {

        }
    }

}
