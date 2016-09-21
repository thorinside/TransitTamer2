package org.nsdev.apps.transittamer.utils;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;

import org.nsdev.apps.transittamer.model.StopRouteSchedule;
import org.nsdev.apps.transittamer.net.model.StopTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.realm.RealmList;
import timber.log.Timber;

/**
 * Created by neal on 2016-09-18.
 */

public class ScheduleUtils {

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);

    public static int getIndexOfNext(StopRouteSchedule schedule) {
        Date now = new Date();
        RealmList<StopTime> stopTimes = schedule.getSchedule();
        int index = 0;
        for (StopTime stopTime : stopTimes) {
            try {
                Date time = timeFormat.parse(String.format("%s %s", dateFormat.format(now), stopTime.getDeparture_time()));

                if (time.after(now)) {
                    Timber.d("Found %s > %s at index %d", time, now, index);
                    return index;
                }

                index++;

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    private static SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static SimpleDateFormat dayFormat2 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public static Date getDateForDepartureTime(Date now, StopTime stopTime) {
        String timeStr = stopTime.getDeparture_time();
        Date date = null;

        String day = dayFormat2.format(now);

        try {
            date = dateFormat2.parse(day + " " + timeStr);
            if (date.getTime() - now.getTime() > (1000 * 60 * 60 * 24)) {
                date.setTime(date.getTime() - (1000 * 60 * 60 * 24));
            }
        } catch (ParseException ex) {
            Log.e("DataManager", "Cannot parse departure time: " + timeStr);
        }

        return date;
    }

    public static String getTimeString(Context context, StopTime stopTime) {
        final Date now = new Date();
        return DateUtils.formatDateTime(context, ScheduleUtils.getDateForDepartureTime(now, stopTime).getTime(), DateUtils.FORMAT_SHOW_TIME);
    }

}
