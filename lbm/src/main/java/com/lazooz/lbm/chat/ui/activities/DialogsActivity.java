package com.lazooz.lbm.chat.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.quickblox.chat.QBChatService;
import com.quickblox.chat.model.QBDialog;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.core.QBEntityCallbackImpl;
import com.quickblox.core.request.QBPagedRequestBuilder;
import com.quickblox.core.request.QBRequestGetBuilder;
import com.lazooz.lbm.chat.ApplicationSingleton;
import com.lazooz.lbm.R;
import com.lazooz.lbm.chat.ui.adapters.DialogsAdapter;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import java.util.ArrayList;
import java.util.List;


public class DialogsActivity extends Activity {

    private ListView dialogsListView;
    private ProgressBar progressBar;
    private static Integer OpponentId;
    private static  String Login ;
    private static String Password;
    private static String OpponentLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      //  setContentView(R.layout.dialogs_activity);

        Intent intent = getIntent();

        OpponentId = intent.getIntExtra("OPPONENTID",0);
        Login      = intent.getStringExtra("USER_LOGIN");
        Password   = intent.getStringExtra("PASSWORD");
        OpponentLogin = intent.getStringExtra("OPPONENT_LOGIN");


        //dialogsListView = (ListView) findViewById(R.id.roomsList);
        //progressBar = (ProgressBar) findViewById(R.id.progressBar);


        // get dialogs
        //
        QBRequestGetBuilder customObjectRequestBuilder = new QBRequestGetBuilder();
        customObjectRequestBuilder.setPagesLimit(100);

        QBChatService.getChatDialogs(null, customObjectRequestBuilder, new QBEntityCallbackImpl<ArrayList<QBDialog>>() {
            @Override
            public void onSuccess(final ArrayList<QBDialog> dialogs, Bundle args) {

                // collect all occupants ids
                //

                List<Integer> usersIDs = new ArrayList<Integer>();
                for (QBDialog dialog : dialogs) {
                    usersIDs.addAll(dialog.getOccupants());
                }

                // Get all occupants info
                //
                QBPagedRequestBuilder requestBuilder = new QBPagedRequestBuilder();
                requestBuilder.setPage(1);
                requestBuilder.setPerPage(usersIDs.size());
                //
                QBUsers.getUsersByIDs(usersIDs, requestBuilder, new QBEntityCallbackImpl<ArrayList<QBUser>>() {
                    @Override
                    public void onSuccess(ArrayList<QBUser> users, Bundle params) {

                        // Save users
                        //
                        ((ApplicationSingleton) getApplication()).setDialogsUsers(users);

                        // build list view
                        //

                        {
                            Bundle bundle = new Bundle();
                            bundle.putInt("OPPONENTID",OpponentId);
                            bundle.putString("OPPONENTLOGIN",OpponentLogin);
                            bundle.putSerializable(ChatActivity.EXTRA_DIALOG, (QBDialog)dialogs.get(0));

                            ChatActivity.start(DialogsActivity.this, bundle);
                        }
                        /*
                        Bundle bundle = new Bundle();
                        bundle.putInt("OPPONENTID",OpponentId);
                        bundle.putString("OPPONENTLOGIN",OpponentLogin);

                        ChatActivity.start(DialogsActivity.this, bundle);
                        */


                        // bundle.putSerializable(ChatActivity.EXTRA_DIALOG, (QBDialog)adapter.getItem(position));

                    }

                    @Override
                    public void onError(List<String> errors) {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(DialogsActivity.this);
                        dialog.setMessage("get occupants errors: " + errors).create().show();
                    }

                });
            }

            @Override
            public void onError(List<String> errors) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(DialogsActivity.this);
                dialog.setMessage("get dialogs errors: " + errors).create().show();
            }
        });
    }


    void buildListView(List<QBDialog> dialogs){
        final DialogsAdapter adapter = new DialogsAdapter(dialogs, DialogsActivity.this);
        dialogsListView.setAdapter(adapter);

      //  progressBar.setVisibility(View.GONE);
        // choose dialog
        //
        dialogsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                QBDialog selectedDialog = (QBDialog) adapter.getItem(position);

                Bundle bundle = new Bundle();
                bundle.putSerializable(ChatActivity.EXTRA_DIALOG, (QBDialog) adapter.getItem(position));

                // group
                if (selectedDialog.getType().equals(QBDialogType.GROUP)) {
                    bundle.putSerializable(ChatActivity.EXTRA_MODE, ChatActivity.Mode.GROUP);

                    // private
                } else {
                    bundle.putSerializable(ChatActivity.EXTRA_MODE, ChatActivity.Mode.PRIVATE);
                }

                // Open chat activity
                //
                bundle.putInt("OPPONENTID",OpponentId);
                bundle.putString("OPPONENTLOGIN",OpponentLogin);
                ChatActivity.start(DialogsActivity.this, bundle);
            }
        });
    }

    void buildListView_my(List<QBDialog> dialogs) {
        final DialogsAdapter adapter = new DialogsAdapter(dialogs, DialogsActivity.this);
        dialogsListView.setAdapter(adapter);

      //  progressBar.setVisibility(View.GONE);
        // choose dialog
        //

        QBDialog selectedDialog = (QBDialog) adapter.getItem(0);

        Bundle bundle = new Bundle();
        bundle.putSerializable(ChatActivity.EXTRA_DIALOG, (QBDialog) adapter.getItem(0));

        // group
        if (selectedDialog.getType().equals(QBDialogType.GROUP)) {
            bundle.putSerializable(ChatActivity.EXTRA_MODE, ChatActivity.Mode.GROUP);

            // private
        } else {
            bundle.putSerializable(ChatActivity.EXTRA_MODE, ChatActivity.Mode.PRIVATE);
        }

        // Open chat activity
        //
        bundle.putInt("OPPONENTID",OpponentId);
        bundle.putString("OPPONENTLOGIN",OpponentLogin);

        ChatActivity.start(DialogsActivity.this, bundle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.rooms, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add) {

            // go to New Dialog activity
            //
            //Intent intent = new Intent(DialogsActivity.this, NewDialogActivity.class);
            //startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
