package org.nsdev.apps.transittamer.modules;

import android.content.SharedPreferences;

import com.squareup.otto.Bus;

import org.nsdev.apps.transittamer.App;
import org.nsdev.apps.transittamer.managers.DataManager;
import org.nsdev.apps.transittamer.managers.ProfileManager;
import org.nsdev.apps.transittamer.net.TransitTamerAPI;

import dagger.Module;
import dagger.Provides;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import retrofit2.Retrofit;

/**
 * Created by neal on 2015-11-30.
 */
@Module
public class UserModule {

    @UserScope
    @Provides
    ProfileManager getProfileManager(SharedPreferences sharedPreferences) {
        return new ProfileManager(sharedPreferences);
    }

    @UserScope
    @Provides
    TransitTamerAPI getTransitTamerAPI(Retrofit retrofit) {
        return retrofit.create(TransitTamerAPI.class);
    }

    @UserScope
    @Provides
    DataManager getDataManager(App context, TransitTamerAPI api, Bus bus, RealmConfiguration config) {
        return new DataManager(context, api, bus, config);
    }

    @UserScope
    @Provides
    ReactiveLocationProvider getLocationProvider(App context) {
        return new ReactiveLocationProvider(context);
    }

    @UserScope
    @Provides
    Bus getBus() {
        return new Bus();
    }

    @UserScope
    @Provides
    RealmConfiguration getRealmConfiguration(App context) {
        RealmConfiguration config = new RealmConfiguration.Builder(context)
                .deleteRealmIfMigrationNeeded()
                .build();

        return config;
    }

    @Provides
    Realm getRealm(RealmConfiguration config) {
        Realm realm = Realm.getInstance(config);
        return realm;
    }

}
