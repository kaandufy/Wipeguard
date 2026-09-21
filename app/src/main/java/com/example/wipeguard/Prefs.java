package com.example.wipeguard;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    public static SharedPreferences get(Context c) {
        Context d = c.createDeviceProtectedStorageContext();
        return d.getSharedPreferences("wipeguard", Context.MODE_PRIVATE);
    }
}
