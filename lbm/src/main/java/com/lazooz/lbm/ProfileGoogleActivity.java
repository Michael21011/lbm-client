package com.lazooz.lbm;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.lazooz.lbm.chat.ui.activities.*;
import com.lazooz.lbm.communications.ServerCom;
import com.lazooz.lbm.preference.MySharedPreferences;
import com.lazooz.lbm.utils.Utils;
import com.quickblox.auth.QBAuth;
import com.quickblox.auth.model.QBSession;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallbackImpl;
import com.quickblox.core.QBSettings;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;
import android.support.v4.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.List;

public class ProfileGoogleActivity extends Activity implements View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<People.LoadPeopleResult> {

    private static final String TAG = "ProfileGoogleActivity";

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

    private boolean mSignInClicked;

    private ConnectionResult mConnectionResult;

    private SignInButton btnSignIn;
    private Button btnSignOut, btnRevokeAccess;
    private ImageView imgProfilePic;
    private TextView txtName, txtEmail,txtDestPlace;
    private LinearLayout llProfileLayout;
    // Profile pic image size in pixels
    private static final int PROFILE_PIC_SIZE = 400;
    private  static boolean mWithoutLogin;
    private  static String mMessage;

    private static String personName;
    private static String personPhotoUrl;
    private static String personGooglePlusProfile;
    private static String email ;
    private static String destination_place_id ;
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

            setContentView(R.layout.activity_profile__google);
            btnSignIn = (SignInButton) findViewById(R.id.btn_sign_in);
            btnSignOut = (Button) findViewById(R.id.btn_sign_out);
            btnRevokeAccess = (Button) findViewById(R.id.btn_revoke_access);




        imgProfilePic = (ImageView) findViewById(R.id.imgProfilePic);
        txtName = (TextView) findViewById(R.id.txtName);
        txtEmail = (TextView) findViewById(R.id.txtEmail);
       // txtDestPlace = (TextView) findViewById(R.id.txtDestPlace);
        llProfileLayout = (LinearLayout) findViewById(R.id.llProfile);


            btnSignIn.setOnClickListener(this);
            btnSignOut.setOnClickListener(this);
            btnRevokeAccess.setOnClickListener(this);
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
        System.out.println("Profile Google OnStart");

            if (mGoogleApiClient == null) {
                mGoogleApiClient = new GoogleApiClient.Builder(this)
                        .addApi(Plus.API)
                        .addScope(Plus.SCOPE_PLUS_LOGIN)
                                // Optionally, add additional APIs and scopes if required.
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
            }
            mGoogleApiClient.connect();

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
            super.onSaveInstanceState(outState);

            outState.putBoolean(KEY_IN_RESOLUTION, mIsInResolution);

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
        mSignInClicked = false;
        Toast.makeText(this, "User is connected!", Toast.LENGTH_LONG).show();

            // Get user's information
            getProfileInformation();

            // Update the UI after signin
            updateUI(true);

    }
    /**
     * Updating the UI, showing/hiding buttons and profile layout
     * */
    private void updateUI(boolean isSignedIn) {
        if (isSignedIn) {
            btnSignIn.setVisibility(View.GONE);
            btnSignOut.setVisibility(View.VISIBLE);
            btnRevokeAccess.setVisibility(View.VISIBLE);
            llProfileLayout.setVisibility(View.VISIBLE);
        } else {
            btnSignIn.setVisibility(View.VISIBLE);
            btnSignOut.setVisibility(View.GONE);
            btnRevokeAccess.setVisibility(View.GONE);
            llProfileLayout.setVisibility(View.GONE);
        }
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

    /**
            * Fetching user's information name, email, profile pic
            * */
    private void getProfileInformation() {
        try {
            Plus.PeopleApi.loadVisible(mGoogleApiClient, null).setResultCallback(this);
            if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
                Person currentPerson = Plus.PeopleApi
                        .getCurrentPerson(mGoogleApiClient);
                 personName = currentPerson.getDisplayName();
                 personPhotoUrl = currentPerson.getImage().getUrl();
                 personGooglePlusProfile = currentPerson.getUrl();
                 email = Plus.AccountApi.getAccountName(mGoogleApiClient);

                Log.e(TAG, "Name: " + personName + ", plusProfile: "
                        + personGooglePlusProfile + ", email: " + email
                        + ", Image: " + personPhotoUrl);

                txtName.setText(personName);
                txtEmail.setText(email);

                // by default the profile url gives 50x50 px image only
                // we can replace the value with whatever dimension we want by
                // replacing sz=X
                personPhotoUrl = personPhotoUrl.substring(0,
                        personPhotoUrl.length() - 2)
                        + PROFILE_PIC_SIZE;

                new LoadProfileImage(imgProfilePic).execute(personPhotoUrl);
                String personNameArray[] = personName.split(" ");
                MySharedPreferences.getInstance().setUserProfile(ProfileGoogleActivity.this,"DONE",personNameArray[0]+"*"+personNameArray[1]);
                signUpQuickBlox(personNameArray[0]+personNameArray[1],"LAZOOZ10");
                //MySharedPreferences.getInstance().setUserProfile(ProfileGoogleActivity.this,"DONE",personNameArray[0]+"*"+personNameArray[1]);
                //SubmitProfileToServer(personName,personPhotoUrl,personGooglePlusProfile,email);

            } else {
                Toast.makeText(getApplicationContext(),
                        "Person information is null", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                Toast.makeText(ProfileGoogleActivity.this, "fail to sign to QuickBlox chat", Toast.LENGTH_LONG).show();
                System.out.println("signUpSignInTask fail");
                //finish();
            }
        });
    }


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
                options.inSampleSize = 2;
                mIcon11 = BitmapFactory.decodeStream(in,null,options);

            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);

        }
    }
    /**
     * Button on click listener
     * */
    @Override
    public void onClick(View v) {
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
    }

    /**
     * Sign-in into google
     * */
    private void signInWithGplus() {
        if (!mGoogleApiClient.isConnecting()) {
            mSignInClicked = true;
            resolveSignInError();
        }
    }

    /**
     * Sign-out from google
     * */
    private void signOutFromGplus() {
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
            updateUI(false);
        }
    }

    /**
     * Revoking access from google
     * */
    private void revokeGplusAccess() {
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status arg0) {
                            Log.e(TAG, "User access revoked!");
                            mGoogleApiClient.connect();
                            updateUI(false);
                        }

                    });
        }
    }
    /**
     * Method to resolve any signin errors
     * */
    private void resolveSignInError() {
        if (mConnectionResult.hasResolution()) {
            try {
                mIntentInProgress = true;
                mConnectionResult.startResolutionForResult(this, RC_SIGN_IN);
            } catch (SendIntentException e) {
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
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

            ServerCom bServerCom = new ServerCom(ProfileGoogleActivity.this);

            String personName  = params[0];
            String personPhotoUrl = params[1];
            String personGooglePlusProfile   = params[2];
            String email    = params[3];
            String chatId    = params[4];



            JSONObject jsonReturnObj=null;
            try {
                MySharedPreferences msp = MySharedPreferences.getInstance();
                bServerCom.setUserProfile(msp.getUserId(ProfileGoogleActivity.this), msp.getUserSecret(ProfileGoogleActivity.this),personName,personPhotoUrl,personGooglePlusProfile,email,chatId);
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
                Toast.makeText(ProfileGoogleActivity.this, "Your profile has been sent to the server...", Toast.LENGTH_LONG).show();
                /*
                String personNameArray[] = personName.split(" ");

                MySharedPreferences.getInstance().setUserProfile(ProfileGoogleActivity.this,"DONE",personNameArray[0]+"*"+personNameArray[1]);
                */
            }
            else if (result.equals("credentials_not_valid")){
                Utils.restartApp(ProfileGoogleActivity.this);
            }
            else
                Toast.makeText(ProfileGoogleActivity.this,"Send profile failed.", Toast.LENGTH_LONG).show();
        }


        @Override
        protected void onPreExecute() {

        }
    }


}
