package com.example.wipeguard;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WipeCountdownActivity extends Activity {

    private static final long DURATION_MS = 3000;

    private RingView ring;
    private TextView bigNumber;
    private TextView terminalLines;
    private TextView bottomMsg;
    private CountDownTimer timer;
    private Vibrator vibrator;
    private final String[] lines = {
            "> oturumlar kapatiliyor...",
            "> anahtarlar imha ediliyor...",
            "> bolumler siliniyor..."
    };

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

        Prefs.get(this).edit().putBoolean("pending_wipe", true).commit();

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrate();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xE6000000);
        root.setClickable(true);
        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent e) { return true; }
        });

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams colLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        colLp.gravity = Gravity.CENTER;
        root.addView(col, colLp);

        FrameLayout ringWrap = new FrameLayout(this);
        int ringSize = dp(220);
        ring = new RingView(this);
        ringWrap.addView(ring, new FrameLayout.LayoutParams(ringSize, ringSize));

        bigNumber = new TextView(this);
        bigNumber.setTextColor(Color.WHITE);
        bigNumber.setTextSize(72);
        bigNumber.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams numLp = new FrameLayout.LayoutParams(ringSize, ringSize);
        ringWrap.addView(bigNumber, numLp);
        col.addView(ringWrap);

        terminalLines = new TextView(this);
        terminalLines.setTextColor(0xFF00FF66);
        terminalLines.setTextSize(13);
        terminalLines.setGravity(Gravity.CENTER);
        terminalLines.setPadding(dp(24), dp(20), dp(24), dp(10));
        col.addView(terminalLines);

        bottomMsg = new TextView(this);
        bottomMsg.setTextColor(Color.WHITE);
        bottomMsg.setTextSize(16);
        bottomMsg.setGravity(Gravity.CENTER);
        bottomMsg.setPadding(dp(28), dp(10), dp(28), 0);
        bottomMsg.setText("\uD83D\uDD12 Bu cihaz \u00e7al\u0131nma riskine kar\u015f\u0131 veri "
                + "g\u00fcvenli\u011fi sa\u011flamak amac\u0131yla formatlan\u0131yor... \uD83D\uDEE1\uFE0F");
        col.addView(bottomMsg);

        setContentView(root);

        timer = new CountDownTimer(DURATION_MS, 30) {
            @Override
            public void onTick(long ms) {
                float progress = 1f - (ms / (float) DURATION_MS);
                ring.setProgress(progress);
                int secLeft = (int) Math.ceil(ms / 1000f);
                if (secLeft < 1) secLeft = 1;
                bigNumber.setText(String.valueOf(secLeft));

                int lineIdx = Math.min(lines.length - 1,
                        (int) (progress * lines.length));
                int pct = (int) (progress * 100);
                terminalLines.setText(lines[lineIdx] + " %" + pct);

                if (ms % 1000 < 40) {
                    glitch();
                }
            }

            @Override
            public void onFinish() {
                ring.setProgress(1f);
                bigNumber.setText("0");
                wipe();
            }
        }.start();
    }

    private void glitch() {
        bigNumber.setTranslationX(dp(4));
        bigNumber.postDelayed(new Runnable() {
            @Override public void run() { bigNumber.setTranslationX(0); }
        }, 60);
    }

    private void vibrate() {
        try {
            if (vibrator == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(400);
            }
        } catch (Exception ignored) { }
    }

    private void wipe() {
        DevicePolicyManager dpm =
                (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, AdminReceiver.class);
        try {
            dpm.wipeData(0);
        } catch (SecurityException e) {
            bottomMsg.setText("Silme ba\u015far\u0131s\u0131z: " + e.getMessage());
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) { return true; }

    @Override
    public void onBackPressed() { }

    static class RingView extends View {
        private float progress = 0f;
        private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fg = new Paint(Paint.ANTI_ALIAS_FLAG);

        RingView(android.content.Context c) {
            super(c);
            bg.setStyle(Paint.Style.STROKE);
            bg.setStrokeWidth(18);
            bg.setColor(0x33FF3B30);
            fg.setStyle(Paint.Style.STROKE);
            fg.setStrokeWidth(18);
            fg.setStrokeCap(Paint.Cap.ROUND);
            fg.setColor(0xFFFF3B30);
        }

        void setProgress(float p) {
            progress = p;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float pad = 20;
            RectF rect = new RectF(pad, pad, getWidth() - pad, getHeight() - pad);
            canvas.drawArc(rect, 0, 360, false, bg);
            canvas.drawArc(rect, -90, 360 * progress, false, fg);
        }
    }
}
