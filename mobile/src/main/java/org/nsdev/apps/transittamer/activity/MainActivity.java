package org.nsdev.apps.transittamer.activity;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.roughike.bottombar.BottomBar;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import org.nsdev.apps.transittamer.App;
import org.nsdev.apps.transittamer.R;
import org.nsdev.apps.transittamer.databinding.ActivityMainBinding;
import org.nsdev.apps.transittamer.fragment.MapFragment;
import org.nsdev.apps.transittamer.fragment.RouteFragment;
import org.nsdev.apps.transittamer.fragment.StopFragment;
import org.nsdev.apps.transittamer.managers.DataManager;
import org.nsdev.apps.transittamer.managers.ProfileManager;
import org.nsdev.apps.transittamer.model.FavouriteStops;
import org.nsdev.apps.transittamer.net.TransitTamerAPI;
import org.nsdev.apps.transittamer.net.model.Stop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmList;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends RxAppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, StopFragment.CoordinatorProvider {

    @Inject
    ProfileManager mProfileManager;

    @Inject
    TransitTamerAPI mApi;

    @Inject
    Realm mRealm;

    @Inject
    DataManager mDataManager;

    private ActivityMainBinding mBinding;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);

        super.onCreate(savedInstanceState);

        ((App) getApplication()).getUserComponent().inject(this);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        Toolbar toolbar = mBinding.toolbar;
        setSupportActionBar(toolbar);

        FloatingActionButton fab = mBinding.fab;
        fab.setOnClickListener(view -> {

            new MaterialDialog.Builder(this)
                    .title(R.string.stops_add_stop)
                    .content(R.string.stops_add_stop_content)
                    .inputType(InputType.TYPE_CLASS_NUMBER)
                    .input(R.string.stops_input_hint, 0, (dialog, input) -> {
                        RealmList<Stop> newStops = new RealmList<>();

                        Observable.from(Arrays.asList(input.toString()))
                                .concatMap(s -> mApi.getStop(s))
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.io())
                                .subscribe(
                                        stop -> {
                                            mRealm.executeTransaction(realm -> {
                                                newStops.add(mRealm.copyToRealmOrUpdate(stop));
                                            });
                                        },
                                        error -> {
                                            Log.e("MainActivity", "Error", error);
                                        },
                                        () -> {
                                            FavouriteStops favouriteStops = mRealm.where(FavouriteStops.class).findFirst();
                                            mRealm.executeTransaction(realm -> {
                                                favouriteStops.getStops().addAll(newStops);
                                                favouriteStops.setLastUpdated(new Date());
                                            });

                                            for (Stop newStop : newStops) {
                                                mDataManager.syncStop(newStop);
                                            }

                                        }
                                );
                    }).show();


        });

        mViewPager = mBinding.viewpager;
        setupViewPager(mViewPager);
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {

        BottomBar bottomNavigation = mBinding.bottomNavigation;

        bottomNavigation.setOnTabSelectListener(tabId -> {
            switch (tabId) {
                case R.id.tab_stops:
                    mBinding.fab.show();
                    mBinding.viewpager.setCurrentItem(0, true);
                    break;
                case R.id.tab_route:
                    mBinding.fab.hide();
                    mBinding.viewpager.setCurrentItem(1, true);
                    break;
                case R.id.tab_map:
                    mBinding.fab.hide();
                    mBinding.viewpager.setCurrentItem(2, true);
                    break;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        return true;
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(StopFragment.newInstance(), "Stop");
        adapter.addFragment(RouteFragment.newInstance(null, null), "Route");
        adapter.addFragment(MapFragment.newInstance(null, null), "Map");
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);
    }

    @Override
    public View getCoordinator() {
        return mBinding.coordinator;
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}
