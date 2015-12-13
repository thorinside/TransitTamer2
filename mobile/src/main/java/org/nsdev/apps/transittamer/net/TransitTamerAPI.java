package org.nsdev.apps.transittamer.net;

import org.nsdev.apps.transittamer.net.model.Agency;

import retrofit.http.GET;
import rx.Observable;

/**
 * API for the TransitTamer service.
 * <p>
 * Created by neal on 2015-11-30.
 */
public interface TransitTamerAPI {
    @GET("agency")
    Observable<Agency> getAgency();
}
