package org.nsdev.apps.transittamer.net.model;

import io.realm.RealmObject;

/**
 * Created by neal on 2015-12-12.
 */
public class ShapePath extends RealmObject {
    private String shape_id;
    private String path;

    public ShapePath() {
    }

    public String getShape_id() {
        return shape_id;
    }

    public void setShape_id(String shape_id) {
        this.shape_id = shape_id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
