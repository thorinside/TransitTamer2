package org.nsdev.apps.transittamer.modules;

import android.content.Context;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import io.realm.RealmObject;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;

/**
 * Module for providing network related dependencies.
 * <p>
 * Created by neal on 2015-11-30.
 */
@Module
public class NetModule {
    private final Context mContext;
    private final String mNetworkEndpoint;

    public NetModule(Context context, String networkEndpoint) {
        mContext = context;
        mNetworkEndpoint = networkEndpoint;
    }

    @Singleton
    @Provides
    public OkHttpClient provideOkHttpClient() {
        // Set up some logging
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Set up some caching
        int cacheSize = 10 * 1024 * 1024; // 10 MiB
        Cache cache = new Cache(mContext.getCacheDir(), cacheSize);

        return new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .cache(cache)
                .build();
    }

    @Singleton
    @Provides
    public Gson provideGson() {
        Gson gson = new GsonBuilder()
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getDeclaringClass().equals(RealmObject.class);
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .create();

        return gson;
    }


    @Singleton
    @Provides
    public Retrofit provideRetrofit(OkHttpClient client, Gson gson) {

        return new Retrofit.Builder()
                .baseUrl(mNetworkEndpoint)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
    }
}
