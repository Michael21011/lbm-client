package com.lazooz.lbm.chat.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import com.quickblox.auth.QBAuth;
import com.quickblox.auth.model.QBSession;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallbackImpl;
import com.quickblox.core.QBSettings;
import com.lazooz.lbm.chat.ApplicationSingleton;
import com.lazooz.lbm.R;
import com.quickblox.users.model.QBUser;

import org.jivesoftware.smack.SmackException;

import java.util.List;

public class SplashActivity extends Activity {

    private static final String APP_ID = "22467";
    private static final String AUTH_KEY = "Bd7VTXM7R8rj93X";
    private static final String AUTH_SECRET = "qukUw5ksyj46qVN";
    //
    private static final String USER_LOGIN = "bobbobbob";
    private static final String USER_PASSWORD = "bobbobbob";

    static final int AUTO_PRESENCE_INTERVAL_IN_SECONDS = 30;

    private QBChatService chatService;
    private static Integer OpponentId;
    private static  String Login ;
    private static String Password;
    private static String OpponentLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_splash);

        Intent intent = getIntent();
        OpponentId = intent.getIntExtra("OPPONENTID",0);
        Login      = intent.getStringExtra("USER_LOGIN");
        Password   = intent.getStringExtra("PASSWORD");
        OpponentLogin = intent.getStringExtra("OPPONENT_LOGIN");
        // Init Chat
        //
        QBChatService.setDebugEnabled(true);
        QBSettings.getInstance().fastConfigInit(APP_ID, AUTH_KEY, AUTH_SECRET);
        if (!QBChatService.isInitialized()) {
            QBChatService.init(this);
        }
        chatService = QBChatService.getInstance();


        // create QB user
        //
        final QBUser user = new QBUser();
        user.setLogin(Login);
        user.setPassword(Password);

        QBAuth.createSession(user, new QBEntityCallbackImpl<QBSession>(){
            @Override
            public void onSuccess(QBSession session, Bundle args) {

                // save current user
                //
                user.setId(session.getUserId());
                ((ApplicationSingleton)getApplication()).setCurrentUser(user);

                // login to Chat
                //
                loginToChat(user);
            }

            @Override
            public void onError(List<String> errors) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(SplashActivity.this);
                dialog.setMessage("create session errors: " + errors).create().show();
            }
        });
    }

    private void loginToChat(final QBUser user){

        chatService.login(user, new QBEntityCallbackImpl() {
            @Override
            public void onSuccess() {

                // Start sending presences
                //
                try {
                    chatService.startAutoSendPresence(AUTO_PRESENCE_INTERVAL_IN_SECONDS);
                } catch (SmackException.NotLoggedInException e) {
                    e.printStackTrace();
                }

                // go to Dialogs screen
                //
                Intent intent = new Intent(SplashActivity.this, DialogsActivity.class);
                intent.putExtra("OPPONENTID",OpponentId);
                intent.putExtra("OPPONENT_LOGIN",OpponentLogin);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(List errors) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(SplashActivity.this);
                dialog.setMessage("chat login errors: " + errors).create().show();
            }
        });
    }
}