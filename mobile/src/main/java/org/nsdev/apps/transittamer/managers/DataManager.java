package org.nsdev.apps.transittamer.managers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.nsdev.apps.transittamer.model.FavouriteStops;
import org.nsdev.apps.transittamer.model.StopRouteSchedule;
import org.nsdev.apps.transittamer.net.TransitTamerAPI;
import org.nsdev.apps.transittamer.net.model.Route;
import org.nsdev.apps.transittamer.net.model.Stop;
import org.nsdev.apps.transittamer.net.model.StopTime;
import org.nsdev.apps.transittamer.utils.ScheduleUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import rx.Observable;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Provides an abstraction of the network data APIs.
 * <p>
 * Created by neal on 2015-11-30.
 */
public class DataManager {
    private static final String PREF_SERVICE = "current_service";
    private final Context mContext;
    private final TransitTamerAPI mApi;
    private final RealmConfiguration mRealmConfiguration;
    private final SharedPreferences mPreferences;

    public DataManager(Context context, TransitTamerAPI api, RealmConfiguration config, SharedPreferences preferences) {
        mContext = context;
        mApi = api;
        mRealmConfiguration = config;
        mPreferences = preferences;
    }

    @SuppressLint("CommitPrefEdits")
    public void syncService(boolean force) {
        if (force) {
            mPreferences.edit().putString(PREF_SERVICE, "").commit();
        }
        syncService();
    }

    public void syncService() {

        mApi.getService()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                        services -> {
                            String service = TextUtils.join(":", services);
                            Timber.i("Service: %s", service);
                            syncService(service);
                        },
                        Timber::e
                );
    }

    // Called on io() thread
    private void syncService(String service) {
        if (!mPreferences.getString(PREF_SERVICE, "").equals(service)) {
            // The previous service doesn't match the current service, so
            // we need to sync all of the stops.
            mPreferences.edit().putString(PREF_SERVICE, service).apply();

            Realm realm = Realm.getInstance(mRealmConfiguration);
            realm.beginTransaction();
            try {

                FavouriteStops favouriteStops = realm.where(FavouriteStops.class).findFirst();

                for (Stop stop : favouriteStops.getStops()) {
                    syncStop(realm, stop);
                }


            } finally {
                realm.commitTransaction();
                realm.close();
            }
        }
    }

    private void syncStop(Realm realm, Stop stop) {

        Timber.i("Syncing stop: %s", stop.getStop_id());

        // Have to clear out any old schedules because they don't
        // match the current service anyway
        stop.getSchedules().deleteAllFromRealm();

        syncStopRoutes(realm, stop);
    }

    private void syncStopRoutes(Realm realm, Stop stop) {
        mApi.getRoutes(stop.getStop_code())
                .observeOn(Schedulers.trampoline())
                .subscribeOn(Schedulers.trampoline())
                .subscribe(routes -> {
                    stop.getRoutes().clear();
                    stop.getSchedules().clear();
                    for (Route route : routes) {
                        stop.getRoutes().add(realm.copyToRealmOrUpdate(route));
                    }
                    syncStopSchedule(realm, stop);
                }, error -> {
                    Timber.e(error, "Getting Stop Routes");
                });
    }

    private void syncStopSchedule(Realm realm, Stop stop) {
        for (Route route : stop.getRoutes()) {
            Timber.i("Route: %s", route.getRoute_long_name());

            mApi.getStopSchedule(stop.getStop_id(), route.getRoute_id())
                    .observeOn(Schedulers.trampoline())
                    .subscribeOn(Schedulers.trampoline())
                    .subscribe(
                            next -> {
                                StopRouteSchedule stopRouteSchedule = new StopRouteSchedule();
                                stopRouteSchedule.setRoute(route);
                                stopRouteSchedule.setStop(stop);
                                RealmList<StopTime> schedule = stopRouteSchedule.getSchedule();
                                for (StopTime stopTime : next) {
                                    schedule.add(realm.copyToRealm(stopTime));
                                }
                                stopRouteSchedule = realm.copyToRealm(stopRouteSchedule);

                                stop.getSchedules().add(stopRouteSchedule);

                                stop.setStopRoutes(getStopRoutes(stop));
                                stop.setNextBus(getNextBus(stop));
                            },
                            error -> {
                                Timber.e(error, "Error syncStopSchedule");
                            }
                    );

            // Also update the trips for the route
            mApi.getTrips(route.getRoute_id())
                    .observeOn(Schedulers.trampoline())
                    .subscribeOn(Schedulers.trampoline())
                    .subscribe(
                            realm::copyToRealmOrUpdate,
                            error -> {
                                Timber.e(error, "Error trips");
                            }
                    );
        }
    }

    public void syncStopNextBus(Stop stop) {
        // Would like to sync in the background, so we need
        // to create a new instance of the stop, on the io thread
        // and do the work there
        Observable.just(stop.getStop_id())
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .map(stopId -> {
                    try (Realm realm = Realm.getInstance(mRealmConfiguration)) {
                        Stop stop1 = realm.where(Stop.class).equalTo("stop_id", stopId).findFirst();
                        if (stop1 != null) {
                            realm.executeTransaction(realm1 -> {
                                stop1.setNextBus(getNextBus(stop1));
                            });
                        }
                        return true;
                    }
                })
                .subscribe(
                        success -> {
                        }, error -> {
                            Log.e("DataManager", "SyncStopNextBus error", error);
                        });
    }

    private String getStopRoutes(Stop stop) {
        RealmList<Route> routes = stop.getRoutes();
        ArrayList<String> shortNames = new ArrayList<>();
        for (Route route : routes) {
            shortNames.add(route.getRoute_short_name());
        }
        StringBuilder builder = new StringBuilder();
        Collections.sort(shortNames, (lhs, rhs) -> Integer.valueOf(lhs).compareTo(Integer.valueOf(rhs)));
        for (String route : shortNames) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(route.trim());
        }
        return builder.toString();
    }

    private String getNextBus(Stop stop) {
        if (stop == null) return "N/A";

        final NextCalculationState state = new NextCalculationState();

        for (StopRouteSchedule schedule : stop.getSchedules()) {
            for (StopTime stopTime : schedule.getSchedule()) {
                Date departureDate = ScheduleUtils.getDateForDepartureTime(state.now, stopTime);
                if (departureDate.after(state.now)) {
                    long diff = departureDate.getTime() - state.now.getTime();
                    if (diff < state.smallestDiff) {
                        synchronized (state) {
                            state.nearestTime = stopTime;
                            state.nearestRoute = schedule.getRoute();
                            state.smallestDiff = diff;
                        }
                    }
                }
            }
        }
        return state.toString();
    }

    private class NextCalculationState {
        final Date now = new Date();
        long smallestDiff = Long.MAX_VALUE;
        StopTime nearestTime;
        Route nearestRoute;

        public String toString() {
            if (nearestRoute == null || nearestTime == null) return "No Service";
            return String.format("#%s: %s", nearestRoute.getRoute_short_name(), ScheduleUtils.getTimeString(mContext, nearestTime));
        }
    }


}
