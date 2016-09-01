package org.nsdev.apps.transittamer.model;

import org.nsdev.apps.transittamer.net.model.Stop;

import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Favourite Stops object
 * Created by nealsanche on 2015-12-29.
 */
public class FavouriteStops extends RealmObject {
    @PrimaryKey
    private int id = 0;
    private RealmList<Stop> stops;
    private Date lastUpdated;

    public FavouriteStops() {
    }

    public RealmList<Stop> getStops() {
        return stops;
    }

    public void setStops(RealmList<Stop> stops) {
        this.stops = stops;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
