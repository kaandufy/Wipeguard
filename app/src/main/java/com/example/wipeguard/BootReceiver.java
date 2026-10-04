package com.example.wipeguard;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String act = intent.getAction();
        if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(act)) {
            DeadMan.check(context);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(act)) {
            if (DeadMan.enabled(context)) DeadMan.schedule(context);
            if (DeadMan.expired(context)) DeadMan.check(context);
            else DeadMan.touch(context, "Acilis");
        }

        SharedPreferences p = Prefs.get(context);
        boolean pending = p.getBoolean("pending_wipe", false);
        boolean test = p.getBoolean("test_mode", true);
        if (!pending || test) return;

        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(context, AdminReceiver.class);
        if (dpm.isAdminActive(admin)) {
            try {
                dpm.wipeData(0);
            } catch (SecurityException e) {
                // nothing else we can do here
            }
        }
    }
}
