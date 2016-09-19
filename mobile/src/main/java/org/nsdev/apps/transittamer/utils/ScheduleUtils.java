package org.nsdev.apps.transittamer.utils;

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
                    Timber.d("Found %s > %s", time, now);
                    return index;
                }

                index++;

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }
}
