package org.nsdev.apps.transittamer.utils;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import org.nsdev.apps.transittamer.model.StopDetailModel;
import org.nsdev.apps.transittamer.model.StopRouteSchedule;
import org.nsdev.apps.transittamer.net.model.StopTime;
import org.nsdev.apps.transittamer.net.model.Trip;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.realm.Realm;
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
        return -1;
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

    public static String getHeadSign(Realm realm, StopRouteSchedule schedule) {
        ArrayList<String> signs = new ArrayList<>();

        for (StopTime stopTime : schedule.getSchedule()) {
            String tripId = stopTime.getTrip_id();
            Trip trip = realm.where(Trip.class)
                    .equalTo("trip_id", tripId)
                    .findFirst();
            if (!signs.contains(trip.getTrip_headsign())) {
                signs.add(trip.getTrip_headsign());
            }

        }
        Collections.sort(signs, (first, second) -> first.compareTo(second));

        return TextUtils.join(" / ", signs);
    }

    public static String getDirectionId(Realm realm, StopRouteSchedule schedule) {
        ArrayList<String> directions = new ArrayList<>();

        for (StopTime stopTime : schedule.getSchedule()) {
            String tripId = stopTime.getTrip_id();
            Trip trip = realm.where(Trip.class)
                    .equalTo("trip_id", tripId)
                    .findFirst();
            if (!directions.contains(trip.getDirection_id())) {
                directions.add(trip.getDirection_id());
            }

        }
        Collections.sort(directions, (first, second) -> first.compareTo(second));

        return TextUtils.join(" / ", directions);
    }

    public static Map<String, String> getShapeIds(Realm realm, StopDetailModel model) {

        HashMap<String, String> routeMap = new HashMap<>();
        HashMap<String, Date> departureTimeMap = new HashMap<>();
        HashMap<String, String> routeNameMap = new HashMap<>();

        for (StopRouteSchedule stopRouteSchedule : model.getSchedules()) {
            int index = getIndexOfNext(stopRouteSchedule);
            if (index != -1) {
                StopTime stopTime = stopRouteSchedule.getSchedule().get(index);
                String tripId = stopTime.getTrip_id();
                Trip trip = realm.where(Trip.class)
                        .equalTo("trip_id", tripId)
                        .findFirst();
                String headSign = trip.getTrip_headsign();
                String shapeId = trip.getShape_id();
                Date departure = getDateForDepartureTime(new Date(), stopTime);
                String routeId = stopRouteSchedule.getRoute().getRoute_id();
                if (departureTimeMap.containsKey(routeId)) {
                    Date otherDeparture = departureTimeMap.get(routeId);
                    if (otherDeparture.before(departure)) {
                        continue;
                    }
                }
                routeMap.put(routeId, shapeId);
                departureTimeMap.put(routeId, departure);

                String routeName = String.format("%s \u2014 %s", stopRouteSchedule.getRoute().getRoute_short_name(), headSign);
                routeNameMap.put(routeName, shapeId);

            }
        }
        return routeNameMap;
    }
}
