package com.lazooz.lbm;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.plus.People;

import com.lazooz.lbm.businessClasses.ServerData;
import com.lazooz.lbm.communications.ServerCom;
import com.lazooz.lbm.preference.MySharedPreferences;

import com.quickblox.auth.QBAuth;
import com.quickblox.auth.model.QBSession;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallbackImpl;
import com.quickblox.core.QBSettings;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RideRequestActivity extends ActionBarActivity implements View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<People.LoadPeopleResult> {

    private static final String TAG = "RideRequestActivity";

    private static final String KEY_IN_RESOLUTION = "is_in_resolution";

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Determines if the client is in a resolution state, and
     * waiting for resolution intent to return.
     */
    private boolean mIsInResolution;
    private boolean mIntentInProgress;
    private static final int RC_SIGN_IN = 0;


    private ConnectionResult mConnectionResult;

    private ImageView imgProfilePic;
    private TextView txtName, txtEmail,txtDestPlace,
                      RideRequestText,WantToRideText,MatchAcceptedText,DurationText;
    private LinearLayout llProfileLayout;
    private LinearLayout maplayout;
    private GoogleMap map;
    private Fragment mapF;

    // Profile pic image size in pixels
    private static final int PROFILE_PIC_SIZE = 400;
    private  static String mMessage;
    private Button AcceptBtn;
    private Button RejectBtn;

    private static String personName;
    private static String personPhotoUrl;
    private static String personGooglePlusProfile;
    private static String email ;
    private static String destination_place_id ;

    private static double User1Lat;
    private static double User1Lo ;

    private static double User2Lat;
    private static double User2Lo ;
    private static String MatchRequestId;
    private static String TypeActivity,Duration;
    private static String Direction;
    private ProgressBar mProgressBar,mProgressBar1;
    private Runnable runnable;
    private Handler handler;
    private int MatchWaitTimeCounter;
    private boolean reject = false;

    /**
     * Called when the activity is starting. Restores the activity state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsInResolution = savedInstanceState.getBoolean(KEY_IN_RESOLUTION, false);
        }

        mMessage = getIntent().getStringExtra("MESSAGE");
        ParseMessage();

        setContentView(R.layout.riderequest);


        imgProfilePic = (ImageView) findViewById(R.id.imgProfilePic);
        txtName = (TextView) findViewById(R.id.txtName);
        txtEmail = (TextView) findViewById(R.id.txtEmail);
        // txtDestPlace = (TextView) findViewById(R.id.txtDestPlace);
        llProfileLayout = (LinearLayout) findViewById(R.id.llProfile);
        RideRequestText = (TextView) findViewById(R.id.ride_request_text);
        WantToRideText = (TextView) findViewById(R.id.want_to_ride_text);
        MatchAcceptedText = (TextView) findViewById(R.id.match_accepted_text);
        MatchAcceptedText.setVisibility(View.GONE);
        mProgressBar = (ProgressBar)findViewById(R.id.ride_progress);
        mProgressBar1 = (ProgressBar)findViewById(R.id.wait_progress);
        DurationText = (TextView)findViewById(R.id.duration);
        maplayout = (LinearLayout) findViewById(R.id.map_layout);
        DurationText.setText(Duration);

        maplayout.setVisibility(View.GONE);
        RideRequestText.setVisibility(View.GONE);
        txtName.setVisibility(View.GONE);
        txtEmail.setVisibility(View.GONE);
        WantToRideText.setVisibility(View.GONE);



       // mProgressBar.setVisibility(View.GONE);


            AcceptBtn = (Button)findViewById(R.id.btn_accept);
            AcceptBtn.setVisibility(View.GONE);
            AcceptBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    SendAcceptMatchToServer(MatchRequestId, "yes");
                    MySharedPreferences msp = MySharedPreferences.getInstance();
                    msp.saveMatchRequestId(RideRequestActivity.this,MatchRequestId);
                    mProgressBar.setVisibility(View.VISIBLE);
                    AcceptBtn.setVisibility(View.GONE);
                    DurationText.setVisibility(View.GONE);

                    /*
                    String mMessageArray[] =  mMessage.split(" ");

                    Integer OpponentId = Integer.valueOf(mMessageArray[10]);
                    Intent intent = new Intent(RideRequestActivity.this, com.lazooz.lbm.chat.ui.activities.SplashChatActivity.class);
                    String ChatLogin = MySharedPreferences.getInstance().getUserProfile(RideRequestActivity.this,"ChatLogin");
                    intent.putExtra("USER_LOGIN",ChatLogin);
                    intent.putExtra("OPPONENT_LOGIN",mMessageArray[1]+mMessageArray[2]);
                    intent.putExtra("PASSWORD","LAZOOZ10");
                    intent.putExtra("OPPONENTID",OpponentId);
                    startActivity(intent);

                    finish();
                    */
                }
            });
            RejectBtn = (Button)findViewById(R.id.btn_reject);
            RejectBtn.setVisibility(View.GONE);
            RejectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (TypeActivity.contains("match_accept")) {

                        reject =  true;
                        SendAcceptMatchToServer(MatchRequestId,"no");

                    }
                    else {
                        Intent intent = new Intent(RideRequestActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }

                }
            });
        if (TypeActivity.contains("match_accept")) {
            RideRequestText.setVisibility(View.GONE);
            WantToRideText.setText("Want to come and pick you up");
            AcceptBtn.setText("Still relevant");
            RejectBtn.setText("No need");

        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_to_rider);

        map = mapFragment.getMap();

        if (mGoogleApiClient == null) {
            rebuildGoogleApiClient();
        }
        mGoogleApiClient.connect();
        ShowUsOnMap();
        runnable = new Runnable() {
            @Override
            public void run() {
			      /* do what you need to do */
                 if (CheckIfMatchAccepted() == false)

			      /* and here comes the "trick" */
                  handler.postDelayed(this, 1000*10);
            }
        };

        handler = new Handler();
        MatchWaitTimeCounter = 0;
        handler.postDelayed(runnable, 1000 * 10);
            // Update the UI after signin
            //updateUI(true);
        }
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }
    public void drawPath(String  result) {

        try {
            //Tranform the string into a json object
            final JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");
            List<LatLng> list = decodePoly(encodedString);

            for(int z = 0; z<list.size()-1;z++){
                LatLng src= list.get(z);
                LatLng dest= list.get(z+1);
                Polyline line = map.addPolyline(new PolylineOptions()
                        .add(new LatLng(src.latitude, src.longitude), new LatLng(dest.latitude,   dest.longitude))
                        .width(3)
                        .color(Color.GREEN).geodesic(true));
            }

        }
        catch (JSONException e) {

        }
    }
    private boolean CheckIfMatchAccepted()
    {

        MySharedPreferences msp = MySharedPreferences.getInstance();
        ServerData sd = msp.getServerData(this);
        if (MatchWaitTimeCounter++ == 30) /* 6*5=30 5 minutes*/
        {
            mProgressBar.setVisibility(View.GONE);
            MatchAcceptedText.setVisibility(View.VISIBLE);
            DurationText.setVisibility(View.GONE);
            MatchAcceptedText.setText("5 minutes pass...try again");
            return true;

        }
        if (sd.getMatchAccepted().contains("yes")) {
            mProgressBar.setVisibility(View.GONE);
            MatchAcceptedText.setVisibility(View.VISIBLE);
            DurationText.setVisibility(View.GONE);
            MatchAcceptedText.setText("Match accepted");
            msp.saveDataFromServerService(this, null, null, null, null, null, "NA");
            return true;
        }
        if (sd.getMatchAccepted().contains("no")) {
            mProgressBar.setVisibility(View.GONE);
            MatchAcceptedText.setVisibility(View.VISIBLE);
            DurationText.setVisibility(View.GONE);
            MatchAcceptedText.setText("Match rejected");
            msp.saveDataFromServerService( this, null, null, null, null,null, "NA");
            return  true;
        }
        return false;
    }
    private void ParseMessage()
    {

        try {

            JSONObject jsonMessage = new JSONObject(mMessage);

        personName = jsonMessage.getString("NAME");
        personPhotoUrl = jsonMessage.getString("PHOTO");
        personGooglePlusProfile = jsonMessage.getString("GOOGLE_PROFILE");
        email = jsonMessage.getString("EMAIL");
        destination_place_id = jsonMessage.getString("DESTINATION_ID");
        User1Lat = jsonMessage.getDouble("LOC_1_LAT");
        User1Lo =  jsonMessage.getDouble("LOC_1_LON");
        User2Lat = jsonMessage.getDouble("LOC_2_LAT");
        User2Lo = jsonMessage.getDouble("LOC_2_LON");
        MatchRequestId = jsonMessage.getString("MATCH_REQ_ID");
        TypeActivity = jsonMessage.getString("TYPE");
        Duration     = jsonMessage.getString("DURATION");
            Direction     = jsonMessage.getString("DIRECTION");




        } catch (JSONException e) {
            e.printStackTrace();
        }


    }
    private void ShowUsOnMap()
    {

        map.setMyLocationEnabled(true);


        Location location = new Location("dummy");
        location.setLatitude(User1Lat);
        location.setLongitude(User1Lo);

        setMapInitLocation(location);

        drawPath(Direction);

        map.addMarker(new MarkerOptions()
                .position(new LatLng(User1Lat, User1Lo))
                .title("You")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        map.addMarker(new MarkerOptions()
                .position(new LatLng(User2Lat, User2Lo))
                .title("Other"));


    }

    private void setMapInitLocation(Location location){
        if (location != null){
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            map.getUiSettings().setZoomControlsEnabled(true);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 14));

        }

    }
    /**
     * Called when the Activity is made visible.
     * A connection to Play Services need to be initiated as
     * soon as the activity is visible. Registers {@code ConnectionCallbacks}
     * and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    @Override
    protected void onStart() {
        super.onStart();

    }

    private void ShowOnMap()
    {

    }

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
     * Called when activity gets invisible. Connection to Play Services needs to
     * be disconnected as soon as an activity is invisible.
     */
    @Override
    protected void onStop() {

            if (mGoogleApiClient != null) {
                mGoogleApiClient.disconnect();
            }

        super.onStop();
    }

    /**
     * Saves the resolution state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {

    }

    /**
     * Handles Google Play Services resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode) {
                case REQUEST_CODE_RESOLUTION:
                    retryConnecting();
                    break;
            }

    }

    private void retryConnecting() {
        mIsInResolution = false;
        if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Called when {@code mGoogleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");

        Toast.makeText(this, "User is connected!", Toast.LENGTH_LONG).show();
        getProfileInformationWithoutLogin();

    }
    /**
     * Called when {@code mGoogleApiClient} connection is suspended.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");

            retryConnecting();

    }
    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // Show a localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(
                    result.getErrorCode(), this, 0, new OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            retryConnecting();
                        }
                    }).show();
            return;
        }
        // If there is an existing resolution error being displayed or a resolution
        // activity has started before, do nothing and wait for resolution
        // progress to be completed.
        if (mIsInResolution) {
            return;
        }
        mIsInResolution = true;
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
            retryConnecting();
        }
    }

    private void signUpQuickBlox(final String UserLogin, final String Password)
    {
        final String APP_ID = "22467";
        final String AUTH_KEY = "Bd7VTXM7R8rj93X";
        final String AUTH_SECRET = "qukUw5ksyj46qVN";

        QBChatService chatService;

        QBSettings.getInstance().fastConfigInit(APP_ID, AUTH_KEY, AUTH_SECRET);
        if (!QBChatService.isInitialized()) {
            QBChatService.init(this);
        }
        chatService = QBChatService.getInstance();

        QBAuth.createSession(new QBEntityCallbackImpl<QBSession>() {
            @Override
            public void onSuccess(QBSession qbSession, Bundle bundle) {
                SignUpUser(UserLogin, Password);

            }

            @Override
            public void onError(List<String> errors) {

            }
        });
    }

    private void SignUpUser(String name ,String Password) {
        QBUser qbUser = new QBUser();
        qbUser.setLogin(name);
        qbUser.setPassword(Password);
        QBUsers.signUpSignInTask(qbUser, new QBEntityCallbackImpl<QBUser>() {
            @Override
            public void onSuccess(QBUser qbUser, Bundle bundle) {

                Integer ChatId = qbUser.getId();

                SubmitProfileToServer(personName,personPhotoUrl,personGooglePlusProfile,email,ChatId.toString());

                //System.out.println("signUpSignInTask ok");
                //Toast.makeText(ProfileGoogleActivity.this, "", Toast.LENGTH_LONG).show();
                // finish();
            }

            @Override
            public void onError(List<String> strings) {
                //progressDialog.hide();
                //DialogUtils.showLong(context, strings.get(0));
                Toast.makeText(RideRequestActivity.this, "fail to sign to QuickBlox chat", Toast.LENGTH_LONG).show();
                System.out.println("signUpSignInTask fail");
                finish();
            }
        });
    }

    private void getProfileInformationWithoutLogin() {
        try {

            Log.e(TAG, "Name: " + personName + ", plusProfile: "
                    + personGooglePlusProfile + ", email: " + email
                    + ", Image: " + personPhotoUrl +",DestinationID: "+destination_place_id);

            txtName.setText(personName);
            //  txtEmail.setText(email);

                 /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
              details about the place.
              */
            if (TypeActivity.contains("match_request")) {
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, destination_place_id);
                placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            }




            // by default the profile url gives 50x50 px image only
            // we can replace the value with whatever dimension we want by
            // replacing sz=X
            personPhotoUrl = personPhotoUrl.substring(0,
                    personPhotoUrl.length() - 2)
                    + PROFILE_PIC_SIZE;


            new LoadProfileImage(imgProfilePic).execute(personPhotoUrl);
            //SubmitProfileToServer(personName,personPhotoUrl,personGooglePlusProfile,email);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


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
                com.lazooz.lbm.logger.Log.e(TAG, "Place query did not complete. Error: " + places.getStatus().toString());

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
            txtEmail.setText(place.getName() + " " + place.getAddress());
            com.lazooz.lbm.logger.Log.i(TAG, "Place details received: " + place.getName());
        }
    };

    @Override
    public void onResult(People.LoadPeopleResult loadPeopleResult) {

    }

    /**
     * Background Async task to load user profile picture from url
     * */
    private class LoadProfileImage extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public LoadProfileImage(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                mIcon11 = BitmapFactory.decodeStream(in,null,options);

            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {

            bmImage.setImageBitmap(result);
                AcceptBtn.setVisibility(View.VISIBLE);
                RejectBtn.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                RideRequestText.setVisibility(View.VISIBLE);

                txtName.setVisibility(View.VISIBLE);
                txtEmail.setVisibility(View.VISIBLE);
                WantToRideText.setVisibility(View.VISIBLE);
            maplayout.setVisibility(View.VISIBLE);
            mProgressBar1.setVisibility(View.GONE);

            //ShowUsOnMap();


        }
    }
    /**
     * Button on click listener
     * */
    @Override
    public void onClick(View v) {
        /*
        switch (v.getId()) {
            case R.id.btn_sign_in:
                // Signin button clicked
                signInWithGplus();
                break;
            case R.id.btn_sign_out:
                // Signout button clicked
                signOutFromGplus();
                break;
            case R.id.btn_revoke_access:
                // Revoke access button clicked
                revokeGplusAccess();
                break;
        }
        */
    }

    protected void SubmitProfileToServer(String personName ,
                                         String personPhotoUrl,
                                         String personGooglePlusProfil,
                                         String email,
                                         String ChatId) {
        SubmitProfileToServer submitProfileToServer = new SubmitProfileToServer();
        submitProfileToServer.execute(personName,personPhotoUrl,personGooglePlusProfil,email,ChatId);

    }

    private class SubmitProfileToServer extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... params) {

            ServerCom bServerCom = new ServerCom(RideRequestActivity.this);

            String personName  = params[0];
            String personPhotoUrl = params[1];
            String personGooglePlusProfile   = params[2];
            String email    = params[3];
            String chatId    = params[4];



            JSONObject jsonReturnObj=null;
            try {
                MySharedPreferences msp = MySharedPreferences.getInstance();
                bServerCom.setUserProfile(msp.getUserId(RideRequestActivity.this), msp.getUserSecret(RideRequestActivity.this), personName, personPhotoUrl, personGooglePlusProfile, email, chatId);
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


        }


        @Override
        protected void onPreExecute() {

        }
    }

    protected void SendAcceptMatchToServer(String MatchRequestId,String Accept) {
        SendAcceptMatchToServer sendAcceptMatchToServer = new SendAcceptMatchToServer();
        sendAcceptMatchToServer.execute(MatchRequestId,Accept);

    }

    private class SendAcceptMatchToServer extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... params) {

            ServerCom bServerCom = new ServerCom(RideRequestActivity.this);

            String MatchRequestId = params[0];
            String Accept = params[1];

            JSONObject jsonReturnObj=null;
            try {
                MySharedPreferences msp = MySharedPreferences.getInstance();
                bServerCom.setAcceptMatchRequest(msp.getUserId(RideRequestActivity.this), msp.getUserSecret(RideRequestActivity.this), MatchRequestId, Accept);
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

            if (reject) {
                Intent intent = new Intent(RideRequestActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }


        @Override
        protected void onPreExecute() {

        }
    }



}
