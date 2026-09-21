package com.example.wipeguard;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class WipeCountdownActivity extends Activity {

    private static final int SECONDS = 8;

    private boolean testMode;
    private TextView counter;
    private TextView message;
    private ProgressBar spinner;
    private CountDownTimer timer;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        testMode = getIntent().getBooleanExtra("force_test", false)
                || Prefs.get(this).getBoolean("test_mode", true);

        if (!testMode) {
            Prefs.get(this).edit().putBoolean("pending_wipe", true).commit();
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(0xB3000000);
        root.setPadding(60, 60, 60, 60);
        root.setClickable(true);
        root.setFocusable(true);
        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent e) {
                return true;
            }
        });

        spinner = new ProgressBar(this);
        spinner.setIndeterminate(true);
        spinner.setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));
        root.addView(spinner, new LinearLayout.LayoutParams(220, 220));

        counter = new TextView(this);
        counter.setTextColor(Color.WHITE);
        counter.setTextSize(48);
        counter.setGravity(Gravity.CENTER);
        counter.setText(String.valueOf(SECONDS));
        counter.setPadding(0, 30, 0, 30);
        root.addView(counter);

        message = new TextView(this);
        message.setTextColor(Color.WHITE);
        message.setTextSize(18);
        message.setGravity(Gravity.CENTER);
        message.setText("\uD83D\uDD12 Bu cihaz çalınma riskine karşı veri güvenliği "
                + "sağlamak amacıyla formatlanıyor... \uD83D\uDEE1\uFE0F"
                + (testMode ? "\n\n\uD83E\uDDEA TEST MODU: silme yapılmayacak" : ""));
        root.addView(message);

        setContentView(root);

        timer = new CountDownTimer(SECONDS * 1000L, 250) {
            @Override
            public void onTick(long ms) {
                counter.setText(String.valueOf((ms + 999) / 1000));
            }

            @Override
            public void onFinish() {
                counter.setText("0");
                done();
            }
        }.start();
    }

    private void done() {
        if (testMode) {
            spinner.setVisibility(View.GONE);
            message.setText("\uD83E\uDDEA Test bitti, hiçbir veri silinmedi \u2705");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 2500);
            return;
        }
        DevicePolicyManager dpm =
                (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, AdminReceiver.class);
        try {
            dpm.wipeData(0);
        } catch (SecurityException e) {
            spinner.setVisibility(View.GONE);
            message.setText("Silme başarısız: " + e.getMessage());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return true;
    }

    @Override
    public void onBackPressed() {
    }
}
