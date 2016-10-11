package org.nsdev.apps.transittamer;

import android.app.Application;
import android.util.Log;

import com.j256.ormlite.dao.Dao;

import org.nsdev.apps.transittamer.legacy.SqlDataHelper;
import org.nsdev.apps.transittamer.legacy.Stop;
import org.nsdev.apps.transittamer.legacy.StopNickname;
import org.nsdev.apps.transittamer.model.FavouriteStops;
import org.nsdev.apps.transittamer.modules.AppModule;
import org.nsdev.apps.transittamer.modules.DaggerNetComponent;
import org.nsdev.apps.transittamer.modules.DaggerUserComponent;
import org.nsdev.apps.transittamer.modules.NetComponent;
import org.nsdev.apps.transittamer.modules.NetModule;
import org.nsdev.apps.transittamer.modules.UserComponent;
import org.nsdev.apps.transittamer.modules.UserModule;
import org.nsdev.apps.transittamer.utils.DataUtils;

import java.sql.SQLException;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import timber.log.Timber;

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

        Realm.init(this);

        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm realm = Realm.getInstance(config);
        if (realm.where(FavouriteStops.class).findAll().size() == 0) {
            migrateLegacyData(realm);
        }
        realm.close();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    private void migrateLegacyData(Realm realm) {
        // Check for legacy database and move its data over to Paper
        SqlDataHelper helper = new SqlDataHelper(this);
        try {
            Dao<Stop, Object> stopDao = helper.getStopDao();
            Dao<StopNickname, Object> stopNicknameDao = helper.getStopNicknameDao();

            RealmList<org.nsdev.apps.transittamer.net.model.Stop> newStops = new RealmList<>();

            for (Stop stop : stopDao.queryForAll()) {
                Log.e(TAG, "Stop: " + stop.toString());
                StopNickname stopNickname = stopNicknameDao.queryForId(stop.getStopId());
                if (stopNickname != null) {
                    stop.setNickName(stopNickname.getNickName());
                }
                Log.e(TAG, "Stop nickname: " + stop.getNickName());

                org.nsdev.apps.transittamer.net.model.Stop newStop = DataUtils.fromLegacy(stop);
                newStop.setNickname(stop.getNickName());
                newStops.add(newStop);
            }

            realm.beginTransaction();
            FavouriteStops favStops = new FavouriteStops();
            favStops.setStops(newStops);
            realm.copyToRealmOrUpdate(favStops);
            realm.commitTransaction();

            Log.e(TAG, "Favourite Stops migrated successfully.");

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
