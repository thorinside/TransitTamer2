package org.nsdev.apps.transittamer.modules;

import android.content.Context;
import android.content.SharedPreferences;

import org.nsdev.apps.transittamer.App;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Top level module mostly for singletons.
 * <p>
 * Created by neal on 2015-11-30.
 */
@Module
public class AppModule {
    private final App mApp;

    public AppModule(App app) {
        mApp = app;
    }

    @Singleton
    @Provides
    SharedPreferences provideSharedPreferences() {
        return mApp.getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
    }
}
