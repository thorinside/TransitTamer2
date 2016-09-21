package org.nsdev.apps.transittamer.model;

import org.nsdev.apps.transittamer.net.model.Stop;
import org.nsdev.apps.transittamer.net.model.StopTime;
import org.nsdev.apps.transittamer.net.model.Trip;

import java.util.ArrayList;
import java.util.TreeMap;

import io.realm.Realm;
import io.realm.RealmList;

/**
 * Created by neal on 2016-09-20.
 */

public class StopDetailModel {
    private final Stop mStop;

    private final ArrayList<StopRouteSchedule> mSchedules;

    public StopDetailModel(Realm realm, Stop stop) {
        mStop = stop;

        TreeMap<String, StopRouteSchedule> headSignTimes = new TreeMap<>();

        for (StopRouteSchedule stopRouteSchedule : stop.getSchedules()) {
            for (StopTime stopTime : stopRouteSchedule.getSchedule()) {
                String tripId = stopTime.getTrip_id();
                Trip trip = realm.where(Trip.class).equalTo("trip_id", tripId).findFirst();

                String headsign = trip.getTrip_headsign();
                if (!headSignTimes.containsKey(headsign)) {
                    StopRouteSchedule value = new StopRouteSchedule();
                    value.setRoute(stopRouteSchedule.getRoute());
                    value.setStop(stop);
                    value.setSchedule(new RealmList<>());
                    headSignTimes.put(headsign, value);
                }

                headSignTimes.get(headsign).getSchedule().add(stopTime);
            }
        }

        mSchedules = new ArrayList<>();

        for (String headsign : headSignTimes.keySet()) {
            mSchedules.add(headSignTimes.get(headsign));
        }
    }

    public ArrayList<StopRouteSchedule> getSchedules() {
        return mSchedules;
    }

    public Stop getStop() {
        return mStop;
    }
}
