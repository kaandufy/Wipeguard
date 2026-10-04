package com.example.wipeguard;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class AdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        SharedPreferences p = Prefs.get(context);
        boolean active = p.getBoolean("active", false);
        if (!active) return;

        Intent i = new Intent(context, WipeCountdownActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(i);
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        Prefs.get(context).edit().putBoolean("pending_wipe", false).commit();
    }
}
