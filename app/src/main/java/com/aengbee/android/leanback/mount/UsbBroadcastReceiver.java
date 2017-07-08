package com.aengbee.android.leanback.mount;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

/**
 * Created by E5 on 2017-06-24.
 */

public class UsbBroadcastReceiver  extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
            SharedPreferences.Editor sharedPreferencesEditor =
                    android.preference.PreferenceManager.getDefaultSharedPreferences(context).edit();
            sharedPreferencesEditor.putString("USB_path", intent.getData().toString().replace("file://",""));
            sharedPreferencesEditor.apply();
            Toast.makeText(context,"MEDIA MOUNTED",Toast.LENGTH_SHORT).show();
        }
        else if (intent.getAction().equals(Intent.ACTION_MEDIA_EJECT)) {
            SharedPreferences.Editor sharedPreferencesEditor =
                    android.preference.PreferenceManager.getDefaultSharedPreferences(context).edit();
            sharedPreferencesEditor.putString("USB_path", "");
            sharedPreferencesEditor.apply();
            Toast.makeText(context,"MEDIA EJECTED",Toast.LENGTH_SHORT).show();
        }
    }

}
