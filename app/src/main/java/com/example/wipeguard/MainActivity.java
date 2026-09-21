package com.example.wipeguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private DevicePolicyManager dpm;
    private ComponentName admin;
    private TextView status;
    private EditText input;
    private Switch testSwitch;

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private Button makeButton(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(l);
        return b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        admin = new ComponentName(this, AdminReceiver.class);

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(20));
        sv.addView(root);

        TextView title = new TextView(this);
        title.setText("WipeGuard");
        title.setTextSize(24);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("Belirlenen sayida yanlis PIN/desen/sifre denemesinde 8 saniyelik "
                + "geri sayim baslar ve veri silinir. Parmak izi ve yuz denemeleri sayilmaz. "
                + "0 = kapali.");
        info.setPadding(0, dp(8), 0, dp(16));
        root.addView(info);

        status = new TextView(this);
        status.setTextSize(15);
        status.setPadding(0, 0, 0, dp(16));
        root.addView(status);

        root.addView(makeButton("1) Yonetici yetkisi ver", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Yanlis PIN denemesinde cihaz verisini silebilmek icin gerekli.");
                startActivity(i);
            }
        }));

        root.addView(makeButton("2) Ust katman izni ver", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            }
        }));

        input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Deneme sayisi");
        input.setText("3");
        root.addView(input);

        testSwitch = new Switch(this);
        testSwitch.setText("Test modu (silmez)");
        testSwitch.setChecked(Prefs.get(this).getBoolean("test_mode", true));
        testSwitch.setPadding(0, dp(8), 0, dp(8));
        root.addView(testSwitch);

        root.addView(makeButton("3) Esigi uygula", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int n;
                try {
                    n = Integer.parseInt(input.getText().toString().trim());
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Gecerli bir sayi gir", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (n < 0) {
                    Toast.makeText(MainActivity.this, "Negatif olamaz", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (n > 0 && n < 3 && !testSwitch.isChecked()) {
                    confirmLow(n);
                } else {
                    apply(n);
                }
            }
        }));

        root.addView(makeButton("Animasyonu dene (silmez)", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, WipeCountdownActivity.class);
                i.putExtra("force_test", true);
                startActivity(i);
            }
        }));

        root.addView(makeButton("Esigi kapat (0)", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                apply(0);
            }
        }));

        setContentView(sv);
    }

    private void confirmLow(final int n) {
        new AlertDialog.Builder(this)
                .setTitle("Dikkat")
                .setMessage(n + " yanlis denemede TUM VERI silinecek. Yedegin var mi ve "
                        + "test modunda denedin mi?")
                .setPositiveButton("Evet, uygula", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        apply(n);
                    }
                })
                .setNegativeButton("Vazgec", null)
                .show();
    }

    @SuppressWarnings("deprecation")
    private void apply(int n) {
        if (!dpm.isAdminActive(admin)) {
            Toast.makeText(this, "Once yonetici yetkisi ver", Toast.LENGTH_LONG).show();
            return;
        }
        boolean test = testSwitch.isChecked();
        Prefs.get(this).edit()
                .putInt("threshold", n)
                .putBoolean("test_mode", test)
                .putInt("fails", 0)
                .putBoolean("pending_wipe", false)
                .commit();
        int sys = (n > 0 && !test) ? n + 2 : 0;
        try {
            dpm.setMaximumFailedPasswordsForWipe(admin, sys);
            Toast.makeText(this, "Kaydedildi: esik " + n + (test ? " (TEST)" : " (GERCEK)"),
                    Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        refresh();
    }

    @SuppressWarnings("deprecation")
    private void refresh() {
        boolean active = dpm.isAdminActive(admin);
        SharedPreferences p = Prefs.get(this);
        String s = "Yonetici: " + (active ? "AKTIF" : "yok")
                + "\nUst katman izni: " + (Settings.canDrawOverlays(this) ? "VAR" : "YOK");
        if (active) {
            s += "\nSistem esigi (yedek): " + dpm.getMaximumFailedPasswordsForWipe(admin);
        }
        s += "\nUygulama esigi: " + p.getInt("threshold", 0)
                + "\nMod: " + (p.getBoolean("test_mode", true) ? "TEST (silmez)" : "GERCEK (siler)");
        status.setText(s);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }
}
