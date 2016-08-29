package org.nsdev.apps.transittamer.net;

import org.nsdev.apps.transittamer.net.model.Agency;
import org.nsdev.apps.transittamer.net.model.Calendar;
import org.nsdev.apps.transittamer.net.model.CalendarDate;
import org.nsdev.apps.transittamer.net.model.Route;
import org.nsdev.apps.transittamer.net.model.ShapePath;
import org.nsdev.apps.transittamer.net.model.Stop;
import org.nsdev.apps.transittamer.net.model.StopTime;

import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;

/**
 * API for the TransitTamer service.
 * <p>
 * Created by neal on 2015-11-30.
 */
public interface TransitTamerAPI {
    @GET("agency")
    Observable<Agency> getAgency();

    @GET("findStop/{stopCode}")
    Observable<Stop> getStop(@Path("stopCode") String stopCode);

    @GET("routes/{stopCode}")
    Observable<List<Route>> getRoutes(@Path("stopCode") String stopCode);

    @GET("calendar")
    Observable<List<Calendar>> getCalendar();

    @GET("calendars/{year}/{month}/{day}")
    Observable<List<Calendar>> getCalendars(@Path("year") String year, @Path("month") String month, @Path("day") String day);

    @GET("calendars")
    Observable<List<Calendar>> getCalendars();

    @GET("exceptions/{date}")
    Observable<List<CalendarDate>> getExceptions(@Path("date") String date);

    @GET("service")
    Observable<List<String>> getService();

    @GET("findroute/{shortName}")
    Observable<List<Route>> getRoute(@Path("shortName") String shortName);

    @GET("stops/{routeId}")
    Observable<List<Stop>> getStopsForRoute(@Path("routeId") String routeId);

    @GET("stops/{lon}/{lat}/{distance}")
    Observable<List<Stop>> getStopsNearby(@Path("lon") double longitude, @Path("lat") double latitude, @Path("distance") double distance);

    @GET("stop/{routeId}/{lon}/{lat}")
    Observable<Stop> getNearestStop(@Path("routeId") String routeId, @Path("lon") double longitude, @Path("lat") double latitude);

    @GET("shape/{routeId}")
    Observable<List<ShapePath>> getShape(@Path("routeId") String routeId);

    @GET("schedule/{stopId}/{routeId}")
    Observable<List<StopTime>> getStopSchedule(@Path("stopId") String stopId, @Path("routeId") String routeId);

    @GET("schedule/{tripId}")
    Observable<List<StopTime>> getTripSchedule(@Path("tripId") String tripId);
}
