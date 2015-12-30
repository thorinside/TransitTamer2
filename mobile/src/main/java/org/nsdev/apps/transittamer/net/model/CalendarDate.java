package org.nsdev.apps.transittamer.net.model;

import io.realm.RealmObject;

/**
 * Created by neal on 2015-12-12.
 */
public class CalendarDate extends RealmObject {
    private String service_id;
    private String date;
    private String exception_type;

    public CalendarDate() {
    }

    public String getService_id() {
        return service_id;
    }

    public void setService_id(String service_id) {
        this.service_id = service_id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getException_type() {
        return exception_type;
    }

    public void setException_type(String exception_type) {
        this.exception_type = exception_type;
    }
}
