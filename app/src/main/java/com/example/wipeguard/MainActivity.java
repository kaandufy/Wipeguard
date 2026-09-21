package com.example.wipeguard;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private DevicePolicyManager dpm;
    private ComponentName admin;
    private TextView adminStatus, overlayStatus;
    private Switch activeSwitch;

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        admin = new ComponentName(this, AdminReceiver.class);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(dp(28), dp(40), dp(28), dp(28));

        TextView title = new TextView(this);
        title.setText("WipeGuard");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("1 yanlis PIN denemesinde cihaz siliniyor.");
        sub.setTextColor(0xFFB0B0B0);
        sub.setPadding(0, dp(6), 0, dp(30));
        root.addView(sub);

        adminStatus = makeRow(root, "Yonetici yetkisi", new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Yanlis PIN denemesinde cihaz verisini silebilmek icin gerekli.");
                startActivity(i);
            }
        });

        overlayStatus = makeRow(root, "Ust katman izni", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            }
        });

        LinearLayout switchRow = new LinearLayout(this);
        switchRow.setOrientation(LinearLayout.HORIZONTAL);
        switchRow.setGravity(Gravity.CENTER_VERTICAL);
        switchRow.setPadding(0, dp(40), 0, 0);

        TextView switchLabel = new TextView(this);
        switchLabel.setText("AKTIF");
        switchLabel.setTextColor(Color.WHITE);
        switchLabel.setTextSize(20);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        switchRow.addView(switchLabel, lp);

        activeSwitch = new Switch(this);
        switchRow.addView(activeSwitch);
        root.addView(switchRow);

        TextView warn = new TextView(this);
        warn.setText("Aktif iken ilk yanlis PIN cihazi kalici olarak siler. Yedeksiz aktif etme.");
        warn.setTextColor(0xFFFF6B6B);
        warn.setPadding(0, dp(16), 0, 0);
        root.addView(warn);

        setContentView(root);

        activeSwitch.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean wantOn = activeSwitch.isChecked();
                if (wantOn && (!dpm.isAdminActive(admin) || !Settings.canDrawOverlays(MainActivity.this))) {
                    activeSwitch.setChecked(false);
                    warn.setText("Once yonetici yetkisi ve ust katman iznini ver.");
                    return;
                }
                if (wantOn) {
                    new android.app.AlertDialog.Builder(MainActivity.this)
                            .setTitle("Emin misin?")
                            .setMessage("Yedegin var mi? Aktif edince ilk yanlis PIN cihazi siler.")
                            .setPositiveButton("Evet, aktif et", new android.content.DialogInterface.OnClickListener() {
                                @Override public void onClick(android.content.DialogInterface d, int w) {
                                    setActive(true);
                                }
                            })
                            .setNegativeButton("Vazgec", new android.content.DialogInterface.OnClickListener() {
                                @Override public void onClick(android.content.DialogInterface d, int w) {
                                    activeSwitch.setChecked(false);
                                }
                            })
                            .show();
                } else {
                    setActive(false);
                }
            }
        });
    }

    private TextView makeRow(LinearLayout parent, String label, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(14), 0, dp(14));
        row.setOnClickListener(onClick);

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(Color.WHITE);
        l.setTextSize(17);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(l, lp);

        TextView status = new TextView(this);
        status.setTextSize(17);
        row.addView(status);

        parent.addView(row);
        return status;
    }

    @SuppressWarnings("deprecation")
    private void setActive(boolean on) {
        Prefs.get(this).edit()
                .putBoolean("active", on)
                .putBoolean("pending_wipe", false)
                .commit();
        try {
            dpm.setMaximumFailedPasswordsForWipe(admin, on ? 3 : 0);
        } catch (SecurityException ignored) { }
        activeSwitch.setChecked(on);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean adminOk = dpm.isAdminActive(admin);
        boolean overlayOk = Settings.canDrawOverlays(this);
        adminStatus.setText(adminOk ? "\u2705" : "\u274C Ver");
        adminStatus.setTextColor(adminOk ? 0xFF34C759 : 0xFFFF6B6B);
        overlayStatus.setText(overlayOk ? "\u2705" : "\u274C Ver");
        overlayStatus.setTextColor(overlayOk ? 0xFF34C759 : 0xFFFF6B6B);

        SharedPreferences p = Prefs.get(this);
        activeSwitch.setChecked(p.getBoolean("active", false));
    }
}
