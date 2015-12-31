package org.nsdev.apps.transittamer.net.model;

import org.nsdev.apps.transittamer.model.StopRouteSchedule;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by neal on 2015-12-12.
 */
public class Stop extends RealmObject {
    @PrimaryKey
    private String stop_id;
    private String stop_code;
    private String stop_name;
    private String stop_desc;
    private double stop_lat;
    private double stop_lon;
    private String zone_id;
    private String stop_url;
    private String location_type;

    private String nickname = "";
    private RealmList<Route> routes = new RealmList<>();
    private RealmList<StopRouteSchedule> schedules = new RealmList<>();

    private String nextBus = "";
    private String stopRoutes = "";

    public Stop() {
    }

    public String getStop_id() {
        return stop_id;
    }

    public void setStop_id(String stop_id) {
        this.stop_id = stop_id;
    }

    public String getStop_code() {
        return stop_code;
    }

    public void setStop_code(String stop_code) {
        this.stop_code = stop_code;
    }

    public String getStop_name() {
        return stop_name;
    }

    public void setStop_name(String stop_name) {
        this.stop_name = stop_name;
    }

    public String getStop_desc() {
        return stop_desc;
    }

    public void setStop_desc(String stop_desc) {
        this.stop_desc = stop_desc;
    }

    public double getStop_lat() {
        return stop_lat;
    }

    public void setStop_lat(double stop_lat) {
        this.stop_lat = stop_lat;
    }

    public double getStop_lon() {
        return stop_lon;
    }

    public void setStop_lon(double stop_lon) {
        this.stop_lon = stop_lon;
    }

    public String getZone_id() {
        return zone_id;
    }

    public void setZone_id(String zone_id) {
        this.zone_id = zone_id;
    }

    public String getStop_url() {
        return stop_url;
    }

    public void setStop_url(String stop_url) {
        this.stop_url = stop_url;
    }

    public String getLocation_type() {
        return location_type;
    }

    public void setLocation_type(String location_type) {
        this.location_type = location_type;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public RealmList<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(RealmList<Route> routes) {
        this.routes = routes;
    }

    public RealmList<StopRouteSchedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(RealmList<StopRouteSchedule> schedules) {
        this.schedules = schedules;
    }

    public String getNextBus() {
        return nextBus;
    }

    public void setNextBus(String nextBus) {
        this.nextBus = nextBus;
    }

    public String getStopRoutes() {
        return stopRoutes;
    }

    public void setStopRoutes(String stopRoutes) {
        this.stopRoutes = stopRoutes;
    }
}
