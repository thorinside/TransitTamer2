package org.nsdev.apps.transittamer.model;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import org.nsdev.apps.transittamer.BR;
import org.nsdev.apps.transittamer.net.model.Stop;

/**
 * ViewModel for a stop.
 */

public class StopViewModel extends BaseObservable {
    private final Stop mStop;
    private boolean mOpen;

    public StopViewModel(Stop stop) {
        mStop = stop;
    }

    public Stop getStop() {
        return mStop;
    }

    @Bindable
    public String getRoutes() {
        return mStop.getStopRoutes();
    }

    @Bindable
    public String getNext() {
        return mStop.getNextBus();
    }

    @Bindable
    public boolean isOpen() {
        return mOpen;
    }

    public void setOpen(boolean open) {
        if (mOpen == open) return;
        mOpen = open;
        notifyPropertyChanged(BR.open);
    }
}
