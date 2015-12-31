package org.nsdev.apps.transittamer.model;

import org.nsdev.apps.transittamer.net.model.Route;
import org.nsdev.apps.transittamer.net.model.Stop;
import org.nsdev.apps.transittamer.net.model.StopTime;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * A record that will store the schedule for a particular route at a given stop.
 *
 * Created by nealsanche on 2015-12-30.
 */
public class StopRouteSchedule extends RealmObject {
    private Stop stop;
    private Route route;
    private RealmList<StopTime> schedule = new RealmList<>();

    public StopRouteSchedule() {
    }

    public Stop getStop() {
        return stop;
    }

    public void setStop(Stop stop) {
        this.stop = stop;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public RealmList<StopTime> getSchedule() {
        return schedule;
    }

    public void setSchedule(RealmList<StopTime> schedule) {
        this.schedule = schedule;
    }
}
