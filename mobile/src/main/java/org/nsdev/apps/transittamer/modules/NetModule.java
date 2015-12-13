package org.nsdev.apps.transittamer.modules;

import android.content.Context;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit.MoshiConverterFactory;
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
        OkHttpClient okHttpClient = new OkHttpClient();

        // Set up some logging
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        okHttpClient.interceptors().add(interceptor);

        // Set up some caching
        int cacheSize = 10 * 1024 * 1024; // 10 MiB
        Cache cache = new Cache(mContext.getCacheDir(), cacheSize);
        okHttpClient.setCache(cache);

        return okHttpClient;
    }

    @Singleton
    @Provides
    public Retrofit provideRetrofit(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(mNetworkEndpoint)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
    }
}
