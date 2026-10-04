package com.example.wipeguard;

import android.app.admin.DevicePolicyManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** 7 gun kurali: son dogru kilit acilisindan bu yana 7 gunden fazla gectiyse iptal kodu sorulur. */
public class DeadMan {
    public static final long LIMIT_MS = 7L * 24 * 60 * 60 * 1000;
    private static final int JOB_ID = 4711;

    public static boolean enabled(Context c) {
        return Prefs.get(c).getBoolean("deadman", false);
    }

    /** Son giris tarihini sadece ileri alir ve giris kaydina yazar. */
    public static void touch(Context c) {
        touch(c, "Arka plan");
    }

    public static void touch(Context c, String src) {
        SharedPreferences p = Prefs.get(c);
        long now = System.currentTimeMillis();
        if (now <= p.getLong("last_unlock", 0)) return;
        p.edit().putLong("last_unlock", now).commit();
        boolean strong = src.equals("PIN") || src.equals("Acilis") || src.equals("Iptal kodu");
        if (strong || now - p.getLong("last_log", 0) > 10 * 60 * 1000L) {
            String old = p.getString("login_log", "");
            String all = now + "|" + src + (old.isEmpty() ? "" : "\n" + old);
            String[] lines = all.split("\n");
            if (lines.length > 30) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 30; i++) {
                    if (i > 0) sb.append('\n');
                    sb.append(lines[i]);
                }
                all = sb.toString();
            }
            p.edit().putString("login_log", all).putLong("last_log", now).commit();
        }
    }

    /** Giris kaydi satirlari: "zaman|kaynak", en yeni en ustte. */
    public static String[] getLog(Context c) {
        String s = Prefs.get(c).getString("login_log", "");
        return s.isEmpty() ? new String[0] : s.split("\n");
    }

    public static void reset(Context c) {
        Prefs.get(c).edit().putLong("last_unlock", System.currentTimeMillis()).commit();
    }

    public static boolean expired(Context c) {
        if (!enabled(c) || !CodeStore.isSet(c)) return false;
        long last = Prefs.get(c).getLong("last_unlock", 0);
        if (last == 0) return false;
        return System.currentTimeMillis() - last > LIMIT_MS;
    }

    /** Sure dolduysa sayac/iptal kodu ekranini acar. */
    public static void check(Context c) {
        if (!expired(c)) return;
        DevicePolicyManager dpm = (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (!dpm.isAdminActive(new ComponentName(c, AdminReceiver.class))) return;
        Intent i = new Intent(c, WipeCountdownActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        c.startActivity(i);
    }

    /** Test icin: tarihi 8 gun geriye alip kontrolu calistirir. */
    public static void testNow(Context c) {
        Prefs.get(c).edit().putLong("last_unlock",
                System.currentTimeMillis() - LIMIT_MS - 3600000L).commit();
        check(c);
    }

    public static void schedule(Context c) {
        JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo info = new JobInfo.Builder(JOB_ID, new ComponentName(c, DeadManJob.class))
                .setPeriodic(15 * 60 * 1000L)
                .setPersisted(true)
                .build();
        js.schedule(info);
    }

    public static void cancel(Context c) {
        JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        js.cancel(JOB_ID);
    }
}
