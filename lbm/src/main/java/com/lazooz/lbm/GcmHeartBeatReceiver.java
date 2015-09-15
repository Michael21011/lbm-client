package com.lazooz.lbm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import com.lazooz.lbm.businessClasses.Contact;
import com.lazooz.lbm.utils.BBUncaughtExceptionHandler;
import com.lazooz.lbm.utils.Utils;

import org.json.JSONArray;

public class GcmHeartBeatReceiver extends BroadcastReceiver {
	private Context mContext;

	public GcmHeartBeatReceiver() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {
        Log.d("GcmHeartBeatReceiver", "Send GCM Heartbeat request to keep conn alive");
        context.sendBroadcast(new Intent("com.google.android.intent.action.GTALK_HEARTBEAT"));
		context.sendBroadcast(new Intent("com.google.android.intent.action.MCS_HEARTBEAT"));
	}


}
