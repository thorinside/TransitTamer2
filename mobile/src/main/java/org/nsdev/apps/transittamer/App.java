package org.nsdev.apps.transittamer;

import android.app.Application;
import android.util.Log;

import com.cesarferreira.rxpaper.RxPaper;
import com.j256.ormlite.dao.Dao;

import org.nsdev.apps.transittamer.legacy.SqlDataHelper;
import org.nsdev.apps.transittamer.legacy.Stop;
import org.nsdev.apps.transittamer.legacy.StopNickname;
import org.nsdev.apps.transittamer.modules.AppModule;
import org.nsdev.apps.transittamer.modules.DaggerNetComponent;
import org.nsdev.apps.transittamer.modules.DaggerUserComponent;
import org.nsdev.apps.transittamer.modules.NetComponent;
import org.nsdev.apps.transittamer.modules.NetModule;
import org.nsdev.apps.transittamer.modules.UserComponent;
import org.nsdev.apps.transittamer.modules.UserModule;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Main application subclass.
 * <p>
 * Created by neal on 2015-11-28.
 */
public class App extends Application {

    private static final String TAG = "App";

    private UserComponent mUserComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        if (mUserComponent == null) {
            NetComponent netComponent = DaggerNetComponent.builder()
                    .appModule(new AppModule(this))
                    .netModule(new NetModule(this, getNetworkEndpoint()))
                    .build();
            mUserComponent = DaggerUserComponent.builder()
                    .netComponent(netComponent)
                    .userModule(new UserModule())
                    .build();
        }

        RxPaper.with(this)
                .exists(Constants.KEY_FAVOURITE_STOPS)
                .subscribe(exists -> {
                    if (!exists) {
                        migrateLegacyData();
                    }
                });
    }

    private void migrateLegacyData() {
        // Check for legacy database and move its data over to Paper
        SqlDataHelper helper = new SqlDataHelper(this);
        try {
            Dao<Stop, Object> stopDao = helper.getStopDao();
            Dao<StopNickname, Object> stopNicknameDao = helper.getStopNicknameDao();

            ArrayList<org.nsdev.apps.transittamer.model.Stop> newStops = new ArrayList<>();

            for (Stop stop : stopDao.queryForAll()) {
                Log.e(TAG, "Stop: " + stop.toString());
                StopNickname stopNickname = stopNicknameDao.queryForId(stop.getStopId());
                if (stopNickname != null) {
                    stop.setNickName(stopNickname.getNickName());
                }
                Log.e(TAG, "Stop nickname: " + stop.getNickName());

                newStops.add(org.nsdev.apps.transittamer.model.Stop.fromLegacy(stop));

            }

            RxPaper.with(this)
                    .write(Constants.KEY_FAVOURITE_STOPS, newStops)
                    .subscribe(success -> {
                        Log.e(TAG, "Favourite Stops migrated successfully.");
                    });

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getNetworkEndpoint() {
        return getResources().getString(R.string.endpoint);
    }

    /**
     * Get the UserComponent to inject a class with
     *
     * @return a UserComponent
     */
    public UserComponent getUserComponent() {
        return mUserComponent;
    }

    /**
     * This is used to inject a component for testing if
     * required.
     *
     * @param component a usercomponent for testing with
     */
    public void setUserComponent(UserComponent component) {
        mUserComponent = component;
    }
}
