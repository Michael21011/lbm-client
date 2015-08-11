/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lazooz.lbm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.gcm.GcmListenerService;

public class LiveDataListenerService extends GcmListenerService {

    private static final String TAG = "LiveDataListenerService";

    private LbmService mLbmService;
    private boolean mBounded;

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {

        Log.d(TAG, "From: " + from);
        String event = data.getString("event");
        Log.d(TAG, "Event: " + event);

        if (event != null && event.equalsIgnoreCase("LiveData")) {
            callLbmService();
            Log.d(TAG, "done call LbmService");
        }
    }


    private void callLbmService() {
        Intent mIntent = new Intent(this, LbmService.class);
        getApplicationContext().bindService(mIntent, mServerConn, BIND_AUTO_CREATE);
        mLbmService.checkEveryLongPeriod();
    }

    protected ServiceConnection mServerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(TAG, "onServiceConnected");
            mBounded = true;
            LbmService.LocalBinder mLocalBinder = (LbmService.LocalBinder) binder;
            mLbmService = mLocalBinder.getServerInstance();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            mBounded = false;
            mLbmService = null;
        }
    };
}
