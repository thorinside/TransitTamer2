package org.nsdev.apps.transittamer.utils;

import org.nsdev.apps.transittamer.legacy.Stop;

/**
 * Created by nealsanche on 2015-12-29.
 */
public class DataUtils {

    public static final org.nsdev.apps.transittamer.net.model.Stop fromLegacy(Stop stop) {

        org.nsdev.apps.transittamer.net.model.Stop newStop = new org.nsdev.apps.transittamer.net.model.Stop();

        newStop.setLocation_type("");
        newStop.setStop_code(stop.getStopCode());
        newStop.setStop_desc(stop.getDescription());
        newStop.setStop_id(stop.getStopId());
        newStop.setStop_lat(stop.getLatitude());
        newStop.setStop_lon(stop.getLongitude());
        newStop.setStop_name(stop.getName());

        return newStop;
    }
}
