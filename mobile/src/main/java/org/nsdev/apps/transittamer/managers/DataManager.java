package org.nsdev.apps.transittamer.managers;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;

import com.cesarferreira.rxpaper.RxPaper;
import com.squareup.otto.Bus;

import org.nsdev.apps.transittamer.events.StopDataChangedEvent;
import org.nsdev.apps.transittamer.model.Stop;
import org.nsdev.apps.transittamer.net.TransitTamerAPI;
import org.nsdev.apps.transittamer.net.model.Route;
import org.nsdev.apps.transittamer.net.model.StopTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Provides an abstraction of the network data APIs.
 * <p>
 * Created by neal on 2015-11-30.
 */
public class DataManager {
    private final Context mContext;
    private final TransitTamerAPI mApi;
    private final Bus mBus;

    public DataManager(Context context, TransitTamerAPI api, Bus bus) {
        mContext = context;
        mApi = api;
        mBus = bus;
    }

    public void syncStop(Stop stop) {
        syncStopRoutes(stop);
    }

    private void syncStopRoutes(Stop stop) {

        mApi.getRoutes(stop.getStopCode()).subscribeOn(Schedulers.io())
                .subscribeOn(Schedulers.computation())
                .subscribe(routes -> {

                    RxPaper.book()
                            .write("routes-" + stop.getStopId(), routes)
                            .subscribe(success -> {
                                syncStopSchedule(stop);
                            });

                }, error -> {
                    Log.e("DataManager", "Getting Stop Routes", error);
                });

    }

    private void syncStopSchedule(Stop stop) {
        RxPaper.book()
                .read("routes-" + stop.getStopId(), new ArrayList<Route>())
                .concatMap(Observable::from)
                .subscribeOn(Schedulers.io())
                .flatMap(route ->
                        mApi.getStopSchedule(stop.getStopId(), route.route_id)
                                .map(l -> new Pair<>(route, l))
                )
                .flatMap(routeSchedule ->
                        RxPaper.book()
                                .write(String.format("schedule-%s-%s", stop.getStopId(), routeSchedule.first.route_id), routeSchedule.second)
                )
                .subscribe(success -> {
                    Log.e("DataManager", "Sync Stop Schedule success: " + success);
                    try {
                        syncNextBus(stop);
                    } catch (Throwable ex) {
                        Log.e("DataManager", "Oops", ex);
                    }
                }, error -> {
                    Log.e("DataManager", "Oops 2", error);
                }, () -> {
                });
    }

    public Observable<String> getStopRoutes(Stop stop) {
        return RxPaper.book()
                .read("routes-" + stop.getStopCode())
                .subscribeOn(Schedulers.io())
                .map(o -> {
                    List<Route> routes = (List<Route>) o;
                    StringBuilder builder = new StringBuilder();
                    Collections.sort(routes, (lhs, rhs) -> Integer.valueOf(lhs.route_short_name).compareTo(Integer.valueOf(rhs.route_short_name)));
                    for (Route route : routes) {
                        if (builder.length() > 0) {
                            builder.append(", ");
                        }
                        builder.append(route.route_short_name.trim());
                    }
                    return builder.toString();
                });
    }

    public Observable<String> getNextBus(Stop stop) {
        return RxPaper.book()
                .read("next-bus-" + stop.getStopCode(), "")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private class NextCalculationState {
        final Date now = new Date();
        long smallestDiff = Long.MAX_VALUE;
        StopTime nearestTime;
        Route nearestRoute;
        boolean complete;

        int count = 0;

        public String toString() {
            if (nearestRoute == null || nearestTime == null) return "No Service";
            return String.format("#%s: %s", nearestRoute.route_short_name, DateUtils.formatDateTime(mContext, getDateForDepartureTime(now, nearestTime).getTime(), DateUtils.FORMAT_SHOW_TIME));
        }
    }

    private class RouteStopTime {
        final StopTime stopTime;
        final Route route;

        public RouteStopTime(StopTime stopTime, Route route) {
            this.stopTime = stopTime;
            this.route = route;
        }
    }

    private void syncNextBus(Stop stop) {
        final NextCalculationState state = new NextCalculationState();

        RxPaper.book()
                .read("routes-" + stop.getStopId(), new ArrayList<Route>())
                .subscribeOn(AndroidSchedulers.mainThread())
                .flatMap(Observable::from)
                .flatMap(route -> RxPaper.book()
                        .read(String.format("schedule-%s-%s", stop.getStopId(), route.route_id), new ArrayList<StopTime>())
                        .map(stopTimes -> new Pair<Route, List<StopTime>>(route, stopTimes))
                )
                .subscribe(
                        route -> {
                            for (StopTime stopTime : route.second) {
                                Date departureDate = getDateForDepartureTime(state.now, stopTime);
                                if (departureDate.after(state.now)) {
                                    long diff = departureDate.getTime() - state.now.getTime();
                                    if (diff < state.smallestDiff) {
                                        synchronized (state) {
                                            state.nearestTime = stopTime;
                                            state.nearestRoute = route.first;
                                            state.smallestDiff = diff;
                                        }
                                    }
                                }
                            }
                            Log.e("NextBus", state.toString() + " " + Thread.currentThread().getName());
                        }, error -> {
                        }, () -> {
                            synchronized (state) {
                                String nextBus = state.toString();
                                RxPaper.book()
                                        .write("next-bus-" + stop.getStopCode(), nextBus)
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(book -> {
                                            AndroidSchedulers.mainThread().createWorker().schedule(() -> mBus.post(new StopDataChangedEvent()), 5, TimeUnit.SECONDS);
                                        }, error -> {
                                            Log.e("DataManager", "oops 5", error);
                                        });
                            }
                        });
    }

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private Date getDateForDepartureTime(Date now, StopTime stopTime) {
        String timeStr = stopTime.departure_time;
        Date date = null;

        String day = dayFormat.format(now);

        try {
            date = dateFormat.parse(day + " " + timeStr);
            if (date.getTime() - now.getTime() > (1000 * 60 * 60 * 24)) {
                date.setTime(date.getTime() - (1000 * 60 * 60 * 24));
            }
        } catch (ParseException ex) {
            Log.e("DataManager", "Cannot parse departure time: " + timeStr);
        }

        return date;
    }
}
