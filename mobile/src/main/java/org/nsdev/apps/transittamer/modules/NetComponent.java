package org.nsdev.apps.transittamer.modules;

import android.content.SharedPreferences;

import javax.inject.Singleton;

import dagger.Component;
import retrofit.Retrofit;

/**
 * Created by neal on 2015-11-30.
 */
@Singleton
@Component(modules = {AppModule.class, NetModule.class})
public interface NetComponent {
    Retrofit retrofit();

    SharedPreferences sharedPreferences();
}
