package org.nsdev.apps.transittamer.managers;

import android.content.SharedPreferences;

/**
 * Created by neal on 2015-11-30.
 */
public class ProfileManager {
    private final SharedPreferences mSharedPreferences;

    public ProfileManager(SharedPreferences sharedPreferences) {
        mSharedPreferences = sharedPreferences;
    }
}
