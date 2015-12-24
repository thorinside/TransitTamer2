package org.nsdev.apps.transittamer.managers;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;

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
import java.util.Date;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

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
        refreshStopRoutes(stop);
    }

    private void refreshStopRoutes(Stop stop) {

        mApi.getRoutes(stop.getStopCode()).subscribeOn(Schedulers.io())
                .subscribeOn(Schedulers.computation())
                .subscribe(routes -> {

                    RxPaper.with(mContext)
                            .write("routes-" + stop.getStopId(), routes)
                            .subscribe(success -> {
                                refreshNext(stop);
                            });

                }, error -> {
                    Log.e("DataManager", "Getting Stop Routes", error);
                });

    }

    public Observable<String> getStopRoutes(Stop stop) {
        return RxPaper.with(mContext)
                .read("routes-" + stop.getStopCode())
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .map(o -> {
                    List<Route> routes = (List<Route>) o;
                    StringBuilder builder = new StringBuilder();
                    for (Route route : routes) {
                        if (builder.length() > 0) {
                            builder.append(", ");
                        }
                        builder.append(route.route_short_name.trim());
                    }
                    return builder.toString();
                });
    }

    private void refreshNext(Stop stop) {
        RxPaper.with(mContext)
                .read("routes-" + stop.getStopId(), new ArrayList<Route>())
                .concatMap(Observable::from)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(route -> {
                    mApi.getStopSchedule(stop.getStopId(), route.route_id)
                            .subscribe(
                                    stopTimes -> {

                                        RxPaper.with(mContext)
                                                .write(String.format("schedule-%s-%s", stop.getStopId(), route.route_id), stopTimes)
                                                .subscribe(success -> {
                                                    Log.e("DataManager", String.format("Got schedule for stop %s, route %s", stop.getStopId(), route.route_id));
                                                }, error -> {
                                                    Log.e("DataMaanger", "Error", error);
                                                });

                                    }, error -> {
                                        Log.e("DataMaanger", "Error", error);
                                    }, () -> {
                                        AndroidSchedulers.mainThread().createWorker().schedule(() -> mBus.post(new StopDataChangedEvent()));
                                    });
                });
    }

    private class NextCalculationState {
        final Date now = new Date();
        long smallestDiff = Long.MAX_VALUE;
        StopTime nearestTime;
        Route nearestRoute;

        public String toString() {
            if (nearestRoute == null || nearestTime == null) return "No Service";
            return String.format("#%s: %s", nearestRoute.route_short_name, DateUtils.formatDateTime(mContext, getDateForDepartureTime(now, nearestTime).getTime(), DateUtils.FORMAT_SHOW_TIME));
        }
    }

    private class RouteStopTime {
        final Route mRoute;
        final StopTime mStopTime;

        private RouteStopTime(Route route, StopTime stopTime) {
            mRoute = route;
            mStopTime = stopTime;
        }

        public Route getRoute() {
            return mRoute;
        }

        public StopTime getStopTime() {
            return mStopTime;
        }
    }

    public Observable<String> getNext(Stop stop) {
        BehaviorSubject<String> observable = BehaviorSubject.create();

        NextCalculationState state = new NextCalculationState();

        RxPaper.with(mContext)
                .read("routes-" + stop.getStopId(), new ArrayList<Route>())
                .concatMapEager(Observable::from)
                .flatMap(route -> RxPaper.with(mContext)
                        .read(String.format("schedule-%s-%s", stop.getStopId(), route.route_id), new ArrayList<StopTime>())
                        .concatMapEager(Observable::from)
                        .filter(stopTime2 -> {
                            Date departureDate = getDateForDepartureTime(state.now, stopTime2);
                            return departureDate.after(state.now);
                        })
                        .doOnNext(stopTime1 -> {
                            Date departureDate = getDateForDepartureTime(state.now, stopTime1);
                            long diff = departureDate.getTime() - state.now.getTime();
                            if (diff < state.smallestDiff) {
                                state.nearestTime = stopTime1;
                                state.nearestRoute = route;
                                state.smallestDiff = diff;
                                Log.d("DataManager", "Got here: " + Thread.currentThread().getName() + " " + state.toString());
                            }
                        })
                        .map(stopTime3 -> new RouteStopTime(route, stopTime3))
                )
                .observeOn(AndroidSchedulers.mainThread())
                .forEach(stopTime -> {
                        },
                        error -> {
                            Log.e("DataManager", "WTF?", error);
                        },
                        () -> {
                            synchronized (state) {
                                observable.onNext(state.toString());
                            }
                            Log.d("DataManager", "OnCompleted: " + Thread.currentThread().getName() + " " + state.toString());
                            observable.onCompleted();
                        }
                );

        return observable;
    }

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");

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
