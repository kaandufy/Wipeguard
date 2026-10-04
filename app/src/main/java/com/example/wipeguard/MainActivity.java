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
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private DevicePolicyManager dpm;
    private ComponentName admin;
    private TextView adminStatus, overlayStatus, codeStatus, warnText;
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
        sub.setText("Yanlis PIN girilirse 7 sn icinde iptal kodunu girmezsen cihaz siliniyor.");
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

        codeStatus = makeRow(root, "Iptal kodu", new View.OnClickListener() {
            @Override public void onClick(View v) { showSetCodeDialog(); }
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

        final TextView warn = new TextView(this);
        warnText = warn;
        warn.setText("Aktif iken yanlis PIN sonrasi iptal kodu girilmezse cihaz kalici olarak silinir. Yedeksiz aktif etme.");
        warn.setTextColor(0xFFFF6B6B);
        warn.setPadding(0, dp(16), 0, 0);
        root.addView(warn);

        setContentView(root);

        activeSwitch.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean wantOn = activeSwitch.isChecked();
                if (wantOn && (!dpm.isAdminActive(admin) || !Settings.canDrawOverlays(MainActivity.this) || !CodeStore.isSet(MainActivity.this))) {
                    activeSwitch.setChecked(false);
                    warn.setText("Once yonetici yetkisi, ust katman izni ve iptal kodunu ayarla.");
                    return;
                }
                if (wantOn) {
                    new android.app.AlertDialog.Builder(MainActivity.this)
                            .setTitle("Emin misin?")
                            .setMessage("Yedegin var mi? Aktif edince yanlis PIN sonrasi 7 sn icinde iptal kodunu girmezsen cihaz silinir.")
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

    private EditText numField(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        return e;
    }

    private void showSetCodeDialog() {
        final EditText a = numField("Yeni iptal kodu (4-12 rakam)");
        final EditText b = numField("Kodu tekrar gir");
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(8), dp(24), 0);
        box.addView(a);
        box.addView(b);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Iptal kodu belirle")
                .setMessage("Telefon PIN'inden farkli bir kod sec.")
                .setView(box)
                .setPositiveButton("Kaydet", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        String x = a.getText().toString();
                        String y = b.getText().toString();
                        if (!x.matches("[0-9]{4,12}")) {
                            warnText.setText("Kod 4-12 rakam olmali.");
                        } else if (!x.equals(y)) {
                            warnText.setText("Kodlar uyusmadi.");
                        } else {
                            CodeStore.set(MainActivity.this, x);
                            warnText.setText("Iptal kodu kaydedildi.");
                            onResume();
                        }
                    }
                })
                .setNegativeButton("Vazgec", null)
                .show();
    }

    @SuppressWarnings("deprecation")
    private void setActive(boolean on) {
        Prefs.get(this).edit()
                .putBoolean("active", on)
                .putBoolean("pending_wipe", false)
                .commit();
        try {
            dpm.setMaximumFailedPasswordsForWipe(admin, 0);
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

        boolean codeOk = CodeStore.isSet(this);
        codeStatus.setText(codeOk ? "\u2705 Degistir" : "\u274C Belirle");
        codeStatus.setTextColor(codeOk ? 0xFF34C759 : 0xFFFF6B6B);
        if (adminOk) {
            try { dpm.setMaximumFailedPasswordsForWipe(admin, 0); } catch (SecurityException ignored) { }
        }

        SharedPreferences p = Prefs.get(this);
        activeSwitch.setChecked(p.getBoolean("active", false));
    }
}
