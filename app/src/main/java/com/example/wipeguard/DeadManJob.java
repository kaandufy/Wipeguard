package com.example.wipeguard;

import android.app.KeyguardManager;
import android.app.job.JobParameters;
import android.app.job.JobService;

public class DeadManJob extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (km != null && !km.isKeyguardLocked()) {
            // Telefon su an acik ve kullaniliyor: son giris tarihi guncellenir.
            DeadMan.touch(this);
        } else {
            DeadMan.check(this);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
