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
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WipeCountdownActivity extends Activity {

    private static final long DURATION_MS = 7000;

    private RingView ring;
    private TextView bigNumber;
    private TextView terminalLines;
    private TextView bottomMsg;
    private TextView dots;
    private final StringBuilder entered = new StringBuilder();
    private int codeLen = 0;
    private boolean done = false;
    private CountDownTimer timer;
    private Vibrator vibrator;
    private final String[] lines = {
            "> kimlik dogrulama bekleniyor...",
            "> oturumlar kapatiliyor...",
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
        int ringSize = dp(150);
        ring = new RingView(this);
        ringWrap.addView(ring, new FrameLayout.LayoutParams(ringSize, ringSize));

        bigNumber = new TextView(this);
        bigNumber.setTextColor(Color.WHITE);
        bigNumber.setTextSize(54);
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

        codeLen = CodeStore.isSet(this) ? CodeStore.length(this) : 0;
        if (codeLen > 0) {
            dots = new TextView(this);
            dots.setTextColor(Color.WHITE);
            dots.setTextSize(22);
            dots.setGravity(Gravity.CENTER);
            dots.setPadding(0, dp(14), 0, dp(6));
            col.addView(dots);
            updateDots();
            buildKeypad(col);
        }

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

    private void updateDots() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < codeLen; i++) {
            sb.append(i < entered.length() ? "\u25CF" : "\u25CB");
            if (i < codeLen - 1) sb.append(' ');
        }
        dots.setText(sb.toString());
    }

    private void buildKeypad(LinearLayout col) {
        String[][] rows = {
                {"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"<", "0", ""}
        };
        for (String[] r : rows) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            for (final String k : r) {
                if (k.isEmpty()) {
                    View sp = new View(this);
                    row.addView(sp, new LinearLayout.LayoutParams(dp(68), dp(48)));
                    continue;
                }
                Button b = new Button(this);
                b.setText(k.equals("<") ? "\u232B" : k);
                b.setTextColor(Color.WHITE);
                b.setTextSize(20);
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(0x33FFFFFF);
                bg.setCornerRadius(dp(24));
                b.setBackground(bg);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(68), dp(48));
                lp.setMargins(dp(6), dp(3), dp(6), dp(3));
                b.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (k.equals("<")) onBack(); else onDigit(k);
                    }
                });
                row.addView(b, lp);
            }
            col.addView(row);
        }
    }

    private void onDigit(String d) {
        if (done || entered.length() >= codeLen) return;
        entered.append(d);
        updateDots();
        if (entered.length() == codeLen) submit();
    }

    private void onBack() {
        if (done || entered.length() == 0) return;
        entered.deleteCharAt(entered.length() - 1);
        updateDots();
    }

    private void submit() {
        done = true;
        if (timer != null) timer.cancel();
        if (CodeStore.verify(this, entered.toString())) {
            Prefs.get(this).edit().putBoolean("pending_wipe", false).commit();
            DeadMan.touch(this, "Iptal kodu");
            finish();
        } else {
            bigNumber.setText("0");
            wipe();
        }
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
