package org.nsdev.apps.transittamer.managers;

import android.content.Context;
import android.util.Log;

import com.squareup.otto.Bus;

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
    private final Context mContext;
    private final TransitTamerAPI mApi;
    private final Bus mBus;
    private final RealmConfiguration mRealmConfiguration;

    public DataManager(Context context, TransitTamerAPI api, Bus bus, RealmConfiguration config) {
        mContext = context;
        mApi = api;
        mBus = bus;
        mRealmConfiguration = config;
    }

    public void syncStop(Stop stop) {

        // Would like to sync in the background, so we need
        // to create a new instance of the stop, on the io thread
        // and do the work there
        Observable.just(stop.getStop_id())
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .map(stopId -> {
                    Realm realm = Realm.getInstance(mRealmConfiguration);
                    Stop stop1 = realm.where(Stop.class).equalTo("stop_id", stopId).findFirst();
                    return stop1;
                })
                .subscribe(this::syncStopRoutes, error -> {
                });
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
                        realm.executeTransaction(realm1 -> {
                            stop1.setNextBus(getNextBus(stop1));
                        });
                        return true;
                    }
                })
                .subscribe(
                        success -> {
                        }, error -> {
                            Log.e("DataManager", "SyncStopNextBus error", error);
                        });
    }

    private void syncStopRoutes(Stop stop) {
        mApi.getRoutes(stop.getStop_code())
                .observeOn(Schedulers.trampoline())
                .subscribeOn(Schedulers.trampoline())
                .subscribe(routes -> {
                    try (Realm realm = Realm.getInstance(mRealmConfiguration)) {
                        realm.executeTransaction(realm1 -> {
                            stop.getRoutes().clear();
                            stop.getSchedules().clear();
                            for (Route route : routes) {
                                stop.getRoutes().add(realm.copyToRealmOrUpdate(route));
                            }
                        });
                    }
                    syncStopSchedule(stop);
                }, error -> {
                    Log.e("DataManager", "Getting Stop Routes", error);
                });
    }

    private void syncStopSchedule(Stop stop) {
        for (Route route : stop.getRoutes()) {
            Log.e("DataManager", "Route: " + route.getRoute_long_name());
            Realm realm = Realm.getInstance(mRealmConfiguration);

            mApi.getStopSchedule(stop.getStop_id(), route.getRoute_id())
                    .observeOn(Schedulers.trampoline())
                    .subscribeOn(Schedulers.trampoline())
                    .subscribe(
                            next -> {
                                realm.executeTransaction(realm1 -> {
                                    StopRouteSchedule stopRouteSchedule = new StopRouteSchedule();
                                    stopRouteSchedule.setRoute(route);
                                    stopRouteSchedule.setStop(stop);
                                    RealmList<StopTime> schedule = stopRouteSchedule.getSchedule();
                                    for (StopTime stopTime : next) {
                                        schedule.add(realm1.copyToRealm(stopTime));
                                    }
                                    stopRouteSchedule = realm1.copyToRealm(stopRouteSchedule);
                                    stop.getSchedules().add(stopRouteSchedule);

                                    stop.setStopRoutes(getStopRoutes(stop));
                                    stop.setNextBus(getNextBus(stop));
                                });
                            },
                            error -> {
                                Log.e("DataManager", "Error syncStopSchedule", error);
                            },
                            () -> {
                            }
                    );

            // Also update the trips for the route
            mApi.getTrips(route.getRoute_id())
                    .observeOn(Schedulers.trampoline())
                    .subscribeOn(Schedulers.trampoline())
                    .subscribe(
                            next -> {
                                realm.executeTransaction(realm1 -> {
                                    realm1.copyToRealmOrUpdate(next);
                                });
                            },
                            error -> {
                                Timber.e(error, "Error trips");
                            },
                            () -> {
                                realm.close();
                            });
        }
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
