package com.lazooz.lbm;


import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.lazooz.lbm.businessClasses.ServerData;
import com.lazooz.lbm.businessClasses.UserNotification;
import com.lazooz.lbm.businessClasses.UserNotificationList;
import com.lazooz.lbm.cfg.StaticParms;
import com.lazooz.lbm.chat.ui.activities.SplashChatActivity;
import com.lazooz.lbm.communications.ServerCom;
import com.lazooz.lbm.preference.MySharedPreferences;
import com.lazooz.lbm.utils.BBUncaughtExceptionHandler;
import com.lazooz.lbm.utils.OfflineActivities;
import com.lazooz.lbm.utils.Utils;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookSdk;

public class MainActivity extends MyActionBarActivity  {


	private Timer ShortPeriodTimer;
	private TextView mDistanceTV;
	private TextView mZoozBalTV;

	private ImageButton mAddFriendsBtn;
    private Button mRideShareBtn;
	private ImageButton mShakeBtn,mRideShareActiveBtn;
	private ProgressBar mCriticalMassPB;
	private LocationManager mLocationManager;
	private TextView mFriendsTV;
	private TextView mShakeTV;
	private ImageButton mDistanceBtn;
	private ImageButton mZoozBalBtn;
	private FrameLayout mCriticalMassFrame;
	private LinearLayout mDistanceLL;
	private LinearLayout mShakeLL;
	private LinearLayout mZoozBalLL;
	private LinearLayout mAddFriendsLL;
	private LinearLayout mRideShareActiveLL;
	//private TextView mConvertionRateTV;
	private TextView mCriticalMassLocationTV;
	protected boolean mUnderCurrentVersionShowed;
	private boolean mUnderMinVersionShowed;
	private AlertDialog.Builder mNoGPSAlertDialog;
	private boolean mNoGPSAlertDialogIsOn = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//super.onCreate(savedInstanceState);
		super.onCreate(savedInstanceState, R.layout.activity_main_new, true);
		//setContentView(R.layout.activity_main);
		
		Thread.setDefaultUncaughtExceptionHandler( new BBUncaughtExceptionHandler(this));

		OfflineActivities.getInstance(this).transmitDataToServer();
		
		MySharedPreferences.getInstance().initFirstTime(this);
		
		if(!Utils.isMyServiceRunning(this, LbmService.class))
			startService(new Intent(this, LbmService.class));

		initNFC();
		
		Utils.setTitleColor(this, getResources().getColor(R.color.white));
		
		
		
		mDistanceTV = (TextView)findViewById(R.id.main_distance_tv);
		
		mDistanceTV.setText("0.0");
		mZoozBalTV = (TextView)findViewById(R.id.main_zoz_balance_tv);
		mZoozBalTV.setText("0.0");
		mFriendsTV = (TextView)findViewById(R.id.main_friends_tv);
		mFriendsTV.setText("0");
		mShakeTV = (TextView)findViewById(R.id.main_shake_tv);
		
		
		//mConvertionRateTV = (TextView)findViewById(R.id.main_zooz_conversion_rate_tv);
		
		mShakeTV.setText("0");
		
		/*************************************************************************/
		mAddFriendsBtn = (ImageButton)findViewById(R.id.main_friends_btn);
		mAddFriendsLL = (LinearLayout)findViewById(R.id.main_friends_ll);
		View.OnClickListener addFriendsListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, MainAddFriendsActivity.class);
				startActivity(intent);				
			}
		};
		mAddFriendsBtn.setOnClickListener(addFriendsListener);
		mAddFriendsLL.setOnClickListener(addFriendsListener);

		/*************************************************************************/
		
		mShakeBtn = (ImageButton)findViewById(R.id.main_shake_btn);
		mShakeLL = (LinearLayout)findViewById(R.id.main_shake_ll);
		View.OnClickListener shakeListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, MainShakeActivity.class);
				startActivity(intent);			
			}
		};
		mShakeBtn.setOnClickListener(shakeListener);
		mShakeLL.setOnClickListener(shakeListener);
		
		/*************************************************************************/
        /*************************************************************************/

        mRideShareBtn = (Button)findViewById(R.id.train_ride_share_button);

        View.OnClickListener RideShareListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {


				MySharedPreferences msp = MySharedPreferences.getInstance();
				ServerData sd = msp.getServerData(MainActivity.this);

				if (Float.valueOf(sd.getPotentialZoozBalance())< StaticParms.RIDE_SHARING_ZOOZ_COST)
				{
					String MsgToUser = getString(R.string.rideshare_not_enogh_credit);
					MsgToUser  = String.format(MsgToUser, StaticParms.RIDE_SHARING_ZOOZ_COST);

					Utils.messageToUser(MainActivity.this,getString(R.string.oops), MsgToUser, MainActivity.this);
					return;
				}


				try {
					int UsersAroundMe = 0;
					JSONObject jsonUsersAroundMeForRideShare = null;

					try {
						jsonUsersAroundMeForRideShare = new JSONObject(sd.getUsersAroundMe());
						UsersAroundMe = Integer.parseInt(jsonUsersAroundMeForRideShare.getString("3_KM"));
					} catch (JSONException e) {
						e.printStackTrace();
					}
					if (UsersAroundMe == 0) {
						String MsgToUser = getString(R.string.ridesharing_no_active_user_around_you);
						Utils.messageToUser(MainActivity.this,getString(R.string.oops), MsgToUser, MainActivity.this);
						return;
					}


				} catch(NumberFormatException nfe) {
					System.out.println("Could not parse " + nfe);
				}

				msp.saveMessageForRideShare(MainActivity.this, "", 0);

                Intent intent = new Intent(MainActivity.this, RideShareEnterRequestActivity.class);

                startActivity(intent);
                finish();

				//TestScreen();
				/*Comment this out for
				TestScreen();
				*/

			}
        };
        mRideShareBtn.setOnClickListener(RideShareListener);

		mRideShareActiveLL = (LinearLayout)findViewById(R.id.rideshare_active_layout);
		mRideShareActiveBtn = (ImageButton)findViewById(R.id.rideshare_active);
		//CheckRideShareActive();

		View.OnClickListener RideShareActiveListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				int State;
				mRideShareActiveBtn.setEnabled(false);
				MySharedPreferences msp = MySharedPreferences.getInstance();
				State = msp.getStateForRideShare(MainActivity.this);
				if (State ==1) {

					Intent intent = new Intent(MainActivity.this, RideRequestActivity.class);
					intent.putExtra("MESSAGE", msp.getMessageForRideShare(MainActivity.this));
					startActivity(intent);
					finish();
				}
				if (State == 2 )
				{
					Intent intent = new Intent(MainActivity.this, RideOnTheWayActivity.class);
					intent.putExtra("MESSAGE", msp.getMessageForRideShare(MainActivity.this));
					startActivity(intent);
					finish();
				}

			}
		};
		mRideShareActiveBtn.setOnClickListener(RideShareActiveListener);
       // mShakeLL.setOnClickListener(shakeListener);

        /*************************************************************************/
		
		mDistanceBtn = (ImageButton)findViewById(R.id.main_distance_btn);
		mDistanceLL = (LinearLayout)findViewById(R.id.main_distance_ll);
		View.OnClickListener distanceListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, MainDistanceActivity.class);
				startActivity(intent);
			}
		};
		mDistanceBtn.setOnClickListener(distanceListener);
		mDistanceLL.setOnClickListener(distanceListener);
		
		/*************************************************************************/
		
		mZoozBalBtn = (ImageButton)findViewById(R.id.main_zoz_balance_btn);
		mZoozBalLL = (LinearLayout)findViewById(R.id.main_zoz_balance_ll);
		View.OnClickListener zoozBalListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, MainZoozActivity.class);
				startActivity(intent);
			}
		};
		mZoozBalBtn.setOnClickListener(zoozBalListener);
		mZoozBalLL.setOnClickListener(zoozBalListener);
		
		/*************************************************************************/
		
		mCriticalMassPB = (ProgressBar)findViewById(R.id.main_critical_mass_pb);
		mCriticalMassPB.setProgress(0);

		mCriticalMassLocationTV = (TextView)findViewById(R.id.main_critical_mass_location_tv);
		mCriticalMassFrame = (FrameLayout)findViewById(R.id.main_critical_mass_frame);
		
		mCriticalMassFrame.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
				intent.putExtra("URL", "http://lazooz.org");
				startActivity(intent);
			}
		});
		
		
		
		 final Handler guiHandler = new Handler();
		 final Runnable guiRunnable = new Runnable() {
		      public void run() {
		         UpdateGUI();
		         checkVersion();
		      }
		   };
		
		
		ShortPeriodTimer = new Timer();
		TimerTask twoSecondsTimerTask = new TimerTask() {
				@Override
				public void run() {
					guiHandler.post(guiRunnable);				
				}
			};
		ShortPeriodTimer.scheduleAtFixedRate(twoSecondsTimerTask, 0, 10 * 1000);

		startOnDayScheduler();
		startGcmHeartBeatScheduler();

		MySharedPreferences.getInstance().setStage(this, MySharedPreferences.STAGE_MAIN);
		//getUserKeyDataAsync();
		FacebookSdk.sdkInitialize(getApplicationContext());
	}

	public void openChat(View view) {
		Log.d("Main", "Open Chat");
        Intent intent = new Intent(this, SplashChatActivity.class);
        intent.putExtra("USER_LOGIN", "chatuser1");
        intent.putExtra("PASSWORD", "chatuser1");
        intent.putExtra("OPPONENT_LOGIN", "chatuser2");
        intent.putExtra("OPPONENTID", 5249824);
        this.startActivity(intent);
	}

	private void CheckRideShareActive()
	{
		MySharedPreferences m_msp = MySharedPreferences.getInstance();
		ServerData sd = m_msp.getServerData(this);
		if ((sd.getMatchAccepted().contains("time_out")==false)&&(m_msp.getStateForRideShare(MainActivity.this) > 0))
		{
			mRideShareActiveLL.setVisibility(View.VISIBLE);
			//mRideShareActiveBtn.setVisibility(View.VISIBLE);
		}
		else
			mRideShareActiveLL.setVisibility(View.GONE);
	}

    private void TestScreen()
	{
		Intent intent;
		JSONObject message = new JSONObject();

		try {
			message.put("NAME","Shay Zluf");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		try {
			message.put("PHOTO","https://lh3.googleusercontent.com/-MGfoqD-y8Zg/AAAAAAAAAAI/AAAAAAAAAEo/6jW_xgUjjOM/photo.jpg?sz=400");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		try {
			message.put("GOOGLE_PROFILE","https://plus.google.com/107797668666573144488");
			message.put("EMAIL","shayz@lazooz.org");
			message.put("OPPONENTID","3739787");
			message.put("DESTINATION_ID","0");
			message.put("LOC_1_LAT",32.855568333333);
			message.put("LOC_1_LON",35.264236666667);
			message.put("LOC_2_LAT",32.836595);
			message.put("LOC_2_LON",35.27148);
			message.put("MATCH_REQ_ID","684");
			message.put("TYPE","match_request");
			message.put("DURATION","21 mins");
			message.put("DIRECTION","0");
		} catch (JSONException e) {
			e.printStackTrace();
		}


		intent = new Intent(MainActivity.this, RideRequestActivity.class);
		//intent = new Intent(MainActivity.this, RideOnTheWayActivity.class);
		intent.putExtra("MESSAGE", message.toString());
		startActivity(intent);


	}
	protected void checkVersion() {
		if(!mUnderMinVersionShowed && MySharedPreferences.getInstance().isUnderMinBuildNum(this)){
			mUnderMinVersionShowed = true;
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
	     	alertDialog.setTitle(getString(R.string.splash_version_check_title));
	     	alertDialog.setMessage(getString(R.string.splash_version_check_les_min));
	        alertDialog.setCanceledOnTouchOutside(false);
		    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Close", new DialogInterface.OnClickListener() {
		    	
		    	@Override
		        public void onClick(DialogInterface dialog, int which) {
	 				 dialog.cancel();
	 				 MainActivity.this.finish();
	 				System.exit(0);
		    	}
		    });
		    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Open Google Play", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog,int which) {
					final Uri marketUri = Uri.parse(StaticParms.PLAY_STORE_APP_LINK_MARKET);
					try {
					    startActivity(new Intent(Intent.ACTION_VIEW, marketUri));
					} catch (android.content.ActivityNotFoundException anfe) {
					    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(StaticParms.PLAY_STORE_APP_LINK)));
					}
	            	dialog.cancel();
	            	MainActivity.this.finish();
	 				System.exit(0);
	            }
	        });
		    alertDialog.show();

		}
		else if (!mUnderCurrentVersionShowed && MySharedPreferences.getInstance().isUnderCurrentBuildNum(this)){
			mUnderCurrentVersionShowed = true;
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
	     	alertDialog.setTitle(getString(R.string.splash_version_check_title));
	     	alertDialog.setMessage(getString(R.string.splash_version_check_les_current));
		    alertDialog.setCanceledOnTouchOutside(false);
		    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Close", new DialogInterface.OnClickListener() {
		    	
		    	@Override
		        public void onClick(DialogInterface dialog, int which) {
	 				 dialog.cancel();
		    	}
		    });
		    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Open Google Play", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog,int which) {
					final Uri marketUri = Uri.parse(StaticParms.PLAY_STORE_APP_LINK_MARKET); 
					try {
					    startActivity(new Intent(Intent.ACTION_VIEW, marketUri));
					} catch (android.content.ActivityNotFoundException anfe) {
					    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(StaticParms.PLAY_STORE_APP_LINK)));
					}
	            	dialog.cancel();
	 				MainActivity.this.finish();
	 				System.exit(0);
	 				
	            }
	        });
		    try {
				alertDialog.show();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}


	@Override
	protected void onResume() {
		super.onResume();
		checkGPS();
		getUserKeyDataAsync();
		checkNotif();
		/*
		this.registerReceiver(mMessageReceiver,
				new IntentFilter("com.lazooz.lbm.UserNotification"));
				*/
		//CheckRideShareActive();
	}

	@Override
	protected void onPause() {
		// Unregister since the activity is not visible
		//this.unregisterReceiver(mMessageReceiver);
		super.onPause();
	}

	// handler for received Intents for the "my-event" event
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Extract data included in the Intent
			String message = intent.getStringExtra("message");
			Log.d("receiver", "Got message: " + message);
			checkNotif();
		}
	};
	
	
	private void checkNotif() {
		UserNotificationList notifList = MySharedPreferences.getInstance().getNotificationsToDisplay(this);
		if (notifList != null){
			for (int i = 0; i< notifList.getNotifications().size(); i++){
				UserNotification notif = notifList.getNotifications().get(i);
				notif.displayPopup(this,MainActivity.this);
			}
		}
	}

	private void checkGPS() {
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		boolean isGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		boolean isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		
		
		if (!isGPSEnabled && isNetworkEnabled)
			showSettingsAlert(getString(R.string.gps_message_no_gps_yes_net));
		else if (!isGPSEnabled && !isNetworkEnabled)
			showSettingsAlert(getString(R.string.gps_message_no_gps_no_net));
		else if (isGPSEnabled && !isNetworkEnabled)
			showSettingsAlertOneTime(getString(R.string.gps_message_yes_gps_no_net));
	}


	private void startOnDayScheduler() {
		
		Intent intent = new Intent(this, AlarmOneDaySchedReciever.class);
		PendingIntent pintent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		
		//alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis() + delay, 24*60*60*1000, pintent);
		//alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis() + delay, 3*60*1000, pintent);
		alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 24 * 60 * 60 * 1000, pintent);
	}

	private void startGcmHeartBeatScheduler() {

		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		Intent gcmHeartBeatIntent = new Intent(this, GcmHeartBeatReceiver.class);
		PendingIntent gcmHeartBeatPendingIntent = PendingIntent.getBroadcast(this, 0, gcmHeartBeatIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarm.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), 2 * 60 * 1000, gcmHeartBeatPendingIntent);//every 2 minutes
	}

	public void showSettingsAlertOneTime(String theMessage){
		
		
		
		if(MySharedPreferences.getInstance().hasNetworkLocationReminderDisplayed(this)) 
			return;
		
		MySharedPreferences.getInstance().setNetworkLocationReminderDisplay(this);
		
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
   	 

		alert.setTitle(getString(R.string.gps_activate_gps_title));
		alert.setMessage(theMessage);
 
		alert.setPositiveButton(getString(R.string.gps_activate_gps_setting), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
            	Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            	startActivity(intent);
            }
        });
 
		alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	mNoGPSAlertDialogIsOn = false;
            	dialog.cancel();
            }
        });
 

		alert.show();
	}
	
	
	public void showSettingsAlert(String theMessage){
		
		if (mNoGPSAlertDialogIsOn)
			return;
		
		
		if(!MySharedPreferences.getInstance().hasTimePassedForGPSReminder(this)) // if it is not the time to show the dialog - return
			return;
		
		mNoGPSAlertDialogIsOn = true;
		
		mNoGPSAlertDialog = new AlertDialog.Builder(this);
   	 
		final View addView = getLayoutInflater().inflate(R.layout.activation_gps_notif_new_dlg, null);
		
		mNoGPSAlertDialog.setView(addView);
		
		RadioGroup rg = (RadioGroup)addView.findViewById(R.id.gps_reminder_rg);
		rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId == R.id.gps_reminder_day_rb){
					
				}
				else if (checkedId == R.id.gps_reminder_week_rb){
					
				}
				else if (checkedId == R.id.gps_reminder_never_rb){
					
				}
			}
		});
		mNoGPSAlertDialog.setTitle(getString(R.string.gps_activate_gps_title));
		mNoGPSAlertDialog.setMessage(theMessage);
 
		mNoGPSAlertDialog.setPositiveButton(getString(R.string.gps_activate_gps_setting), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
            	RadioButton rbDay = (RadioButton)addView.findViewById(R.id.gps_reminder_day_rb);
            	RadioButton rbWeek = (RadioButton)addView.findViewById(R.id.gps_reminder_week_rb);
            	RadioButton rbNever = (RadioButton)addView.findViewById(R.id.gps_reminder_never_rb);
            	MySharedPreferences.getInstance().activateGPSReminder(MainActivity.this, rbDay.isChecked(), rbWeek.isChecked(), rbNever.isChecked());
            	mNoGPSAlertDialogIsOn = false;
            	Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            	startActivity(intent);
            }
        });
 
		mNoGPSAlertDialog.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	RadioButton rbDay = (RadioButton)addView.findViewById(R.id.gps_reminder_day_rb);
            	RadioButton rbWeek = (RadioButton)addView.findViewById(R.id.gps_reminder_week_rb);
            	RadioButton rbNever = (RadioButton)addView.findViewById(R.id.gps_reminder_never_rb);
            	MySharedPreferences.getInstance().activateGPSReminder(MainActivity.this, rbDay.isChecked(), rbWeek.isChecked(), rbNever.isChecked());
            	mNoGPSAlertDialogIsOn = false;
            	dialog.cancel();
            }
        });
 
		mNoGPSAlertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				mNoGPSAlertDialogIsOn = false;
			}
		});

		mNoGPSAlertDialog.show();
	}
	protected void UpdateGUI() {

		MySharedPreferences msp = MySharedPreferences.getInstance();
		ServerData sd = msp.getServerData(this);
		
		float distanceFromServer = sd.getDistanceFloat();
		float distanceLocal = msp.getLocalDistance(this);
		float distanceTotal = distanceFromServer + distanceLocal;
		float distanceKMf = distanceTotal / 1000;

		
		//mDistanceTV.setText(String.format("%dkm  %dm , l=%d", distanceKMd, distanceMd, localDist));
		//mDistanceTV.setText(String.format("%dkm  %dm", distanceKMd, distanceMd));
		mDistanceTV.setText(String.format("%.1f", distanceKMf));

		
		//mDistanceTV.setText(sd.getDistance());
		float pzb = Float.valueOf(sd.getPotentialZoozBalance());
		mZoozBalTV.setText(String.format("%.2f", pzb));

		JSONObject jsonUsersAroundMe = null;

		try {
			jsonUsersAroundMe = new JSONObject(sd.getUsersAroundMe());
			mShakeTV.setText(jsonUsersAroundMe.getString("3_KM"));
		} catch (JSONException e) {
			e.printStackTrace();
		}


		int numInvitedContacts = msp.getNumInvitedContacts(this);
		int numShakedUsers = msp.getNumShakedUsers(this);
		int criticalMass = msp.getCriticalMass(this);
		
		mFriendsTV.setText(numInvitedContacts + "");
		//mShakeTV.setText(numShakedUsers +"");
		mCriticalMassPB.setProgress(criticalMass);
		mCriticalMassPB.setMax(100);

		if ((sd.getMatchAccepted().contains("time_out")==false)&&(msp.getStateForRideShare(MainActivity.this) > 0))
		{
			mRideShareActiveLL.setVisibility(View.VISIBLE);
			//mRideShareActiveBtn.setVisibility(View.VISIBLE);
		}
		else
			mRideShareActiveLL.setVisibility(View.GONE);

		checkNotif();

		/*
		String dolarConvertionRate = msp.getDolarConvertionRate(this);
		if (dolarConvertionRate.equals(""))
			mConvertionRateTV.setText("");
		else
			mConvertionRateTV.setText("1=$" + dolarConvertionRate);
			*/

	}
	

	

	
	
	
	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}*/

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
			if (mProgBar != null)
				mProgBar.setVisibility(View.GONE);
		    if (requestCode == 1) {
		        if(resultCode == RESULT_OK){
		        //	String fromActivity = data.getStringExtra("ACTIVITY");
		        	String theMessage = data.getStringExtra("MESSAGE");
		        //	String s = MySharedPreferences.getInstance().getRecommendUserList(this).toString();
		        //    Log.e("aaa", s);
		            sendFriendRecommendToServerAsync(theMessage);
		        }
		        if (resultCode == RESULT_CANCELED) {
		            //Write your code if there's no result
		        }
		    }
	}

	
	
	@SuppressLint("NewApi")
	private void initNFC(){
		  NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);// (only need to do this once)
		  if (nfc != null) { // in case there is no NFC
		  // create an NDEF message containing the current URL:
		  MySharedPreferences msp = MySharedPreferences.getInstance();

		  NdefRecord rec = NdefRecord.createUri(String.format(StaticParms.PLAY_STORE_APP_LINK_FORMAT,"nfc",msp.getPublicKey(this),msp.getUserId(this))); // url: current URL (String or Uri)
		  NdefMessage ndef = new NdefMessage(rec);
		  // make it available via Android Beam:
		  nfc.setNdefPushMessage(ndef, this);
		  }
	  }
	
	
	
	
	private void sendFriendRecommendToServerAsync(String theMessage){
		FriendRecommendToServer friendRecommendToServer = new FriendRecommendToServer();
		friendRecommendToServer.execute(theMessage);
	}

	
	
	private class FriendRecommendToServer extends AsyncTask<String, Void, String> {


		@Override
		protected String doInBackground(String... params) {
			
          	ServerCom bServerCom = new ServerCom(MainActivity.this);
          	String theMessage = params[0];
              
        	JSONObject jsonReturnObj=null;
			try {
				MySharedPreferences msp = MySharedPreferences.getInstance();
				
				JSONArray dataList = msp.getRecommendUserList(MainActivity.this);
				
				bServerCom.setFriendRecommend(msp.getUserId(MainActivity.this), msp.getUserSecret(MainActivity.this), dataList.toString(), theMessage);
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
				Toast.makeText(MainActivity.this, "Friend List Sent", Toast.LENGTH_LONG).show();
				startActivity(new Intent(MainActivity.this, CongratulationsGetFriendsActivity.class));
			}
			else if (result.equals("credentials_not_valid")){
				Utils.restartApp(MainActivity.this);
			}
			else if (result.equals("ConnectionError")){
				Utils.displayConnectionError(MainActivity.this, null);
			}
		}
			
		
		@Override
		protected void onPreExecute() {
			
		}
	}

	
	private void getUserKeyDataAsync(){
		GetUserKeyData getUserKeyData = new GetUserKeyData();
		getUserKeyData.execute();
	}
	
	private class GetUserKeyData extends AsyncTask<String, Void, String> {


		@Override
		protected String doInBackground(String... params) {
			
          	ServerCom bServerCom = new ServerCom(MainActivity.this);
        	
              
        	JSONObject jsonReturnObj=null;
			try {
				MySharedPreferences msp = MySharedPreferences.getInstance();
				
				bServerCom.getUserKeyData(msp.getUserId(MainActivity.this), msp.getUserSecret(MainActivity.this));
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
						String zoozBalance = jsonReturnObj.getString("zooz_balance");
						String potentialZoozBalance = jsonReturnObj.getString("potential_zooz_balance");
						String dolarConvertionRate = jsonReturnObj.getString("zooz_to_dolar_conversion_rate");
						String distance = jsonReturnObj.getString("zooz_distance_balance");
						String serverVer = jsonReturnObj.getString("server_version");
						boolean isDistanceAchievement = Utils.yesNoToBoolean(jsonReturnObj.getString("is_distance_achievement"));
						String walletNum = jsonReturnObj.getString("wallet_num");
						int numShakedUsers = jsonReturnObj.getInt("num_shaked_users");
						int numInvitedContacts = jsonReturnObj.getInt("num_invited_contacts");
						int criticalMass = jsonReturnObj.getInt("critical_mass_tab");
						String userId = jsonReturnObj.getString("user_id");
						
						
						
						MySharedPreferences.getInstance().saveDataFromServer1(MainActivity.this, zoozBalance, potentialZoozBalance, distance, 
								isDistanceAchievement, serverVer, walletNum, numShakedUsers, numInvitedContacts, criticalMass, dolarConvertionRate, userId);
						
						
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
				UpdateGUI();
			}
			else if (result.equals("credentials_not_valid")){
				Utils.restartApp(MainActivity.this);
			}
			else if (result.equals("ConnectionError")){
				Utils.displayConnectionError(MainActivity.this, null);
			}

		}
			
		
		@Override
		protected void onPreExecute() {
			
		}
	}


	

	
	
}
