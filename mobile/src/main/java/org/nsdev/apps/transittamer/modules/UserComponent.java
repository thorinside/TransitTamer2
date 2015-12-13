package org.nsdev.apps.transittamer.modules;

import org.nsdev.apps.transittamer.activity.MainActivity;

import dagger.Component;

/**
 * Created by neal on 2015-11-30.
 */
@UserScope
@Component(dependencies = NetComponent.class, modules = UserModule.class)
public interface UserComponent {
    void inject(MainActivity mainActivity);
}
