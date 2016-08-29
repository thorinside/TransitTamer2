package org.nsdev.apps.transittamer.utils;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import java.util.Calendar;

/**
 * A timer that tries to fire as close to the
 * top of the minute as possible. Uses handlers
 * not alarms, so use the lifecycle methods.
 * <p>
 * Created by nealsanche on 2015-12-31.
 */
public class OneMinuteTimer {

    private Handler mHandler;
    private Runnable mOneMinuteTimer;

    public void onCreate(Runnable toExecute) {
        mHandler = new Handler();
        mOneMinuteTimer = () -> {
            try {
                toExecute.run();
            } catch (Throwable ex) {
                Log.e("OneMinuteTimer", "Error detected.", ex);
            } finally {
                mHandler.postAtTime(mOneMinuteTimer, calculateTopOfMinute());
            }
        };
        mHandler.postAtTime(mOneMinuteTimer, calculateTopOfMinute());
    }

    private long calculateTopOfMinute() {
        Calendar c = Calendar.getInstance();
        long uptimeMillis = SystemClock.uptimeMillis();

        int currentSecond = c.get(Calendar.SECOND);
        int seconds = 60 - currentSecond;

        int currentMillisecond = c.get(Calendar.MILLISECOND);
        int millis = 1000 - currentMillisecond;

        int totalMs = (seconds - 1) * 1000 + millis;

        return uptimeMillis + totalMs;
    }

    public void onPause() {
        mHandler.removeCallbacks(mOneMinuteTimer);
    }

    public void onResume() {
        mOneMinuteTimer.run();
    }

    public void onDestroy() {
        mHandler.removeCallbacks(mOneMinuteTimer);
        mOneMinuteTimer = () -> {};
    }
}
