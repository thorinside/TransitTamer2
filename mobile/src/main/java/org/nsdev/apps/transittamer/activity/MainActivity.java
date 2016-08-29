package org.nsdev.apps.transittamer.activity;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;

import com.afollestad.materialdialogs.MaterialDialog;
import com.cesarferreira.rxpaper.RxPaper;
import com.squareup.otto.Bus;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import org.nsdev.apps.transittamer.App;
import org.nsdev.apps.transittamer.Constants;
import org.nsdev.apps.transittamer.R;
import org.nsdev.apps.transittamer.databinding.ActivityMainBinding;
import org.nsdev.apps.transittamer.events.FavouriteStopsChangedEvent;
import org.nsdev.apps.transittamer.fragment.StopFragment;
import org.nsdev.apps.transittamer.managers.ProfileManager;
import org.nsdev.apps.transittamer.model.Stop;
import org.nsdev.apps.transittamer.net.TransitTamerAPI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class MainActivity extends RxAppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    @Inject
    ProfileManager mProfileManager;

    @Inject
    TransitTamerAPI mApi;
    private ActivityMainBinding mBinding;

    @Inject
    Bus mBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((App) getApplication()).getUserComponent().inject(this);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        Toolbar toolbar = mBinding.appBar.toolbar;
        setSupportActionBar(toolbar);

        FloatingActionButton fab = mBinding.appBar.fab;
        fab.setOnClickListener(view -> {

            /*
            mApi.getAgency()
                    .compose(bindToLifecycle())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(agency -> {
                        Agency a = (Agency) agency;
                        Snackbar.make(view, "Got agency: " + a.agency_name, Snackbar.LENGTH_LONG)
                                .setAction("Action", null)
                                .show();
                    }, throwable -> {

                        if (throwable instanceof HttpException) {
                            HttpException httpException = (HttpException) throwable;

                            Snackbar.make(view, "Hmm: " + httpException.code(), Snackbar.LENGTH_LONG)
                                    .setAction("Action", null)
                                    .show();

                        }
                    });


            ArrayList<Stop> newStops = new ArrayList<>();

            Observable.from(Arrays.asList("6604", "6475", "7600", "6602", "6601", "6651", "5999", "8418", "6654", "5150", "3951", "5078", "9820"))
                    .observeOn(Schedulers.io())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .concatMap(s -> mApi.getStop(s))
                    .doOnNext(stop -> newStops.add(Stop.fromNetModel(stop)))
                    .doOnCompleted(() -> {
                        RxPaper.with(this)
                                .write(Constants.KEY_FAVOURITE_STOPS, newStops)
                                .subscribe(success -> {
                                    Log.e(TAG, "Favourite Stops migrated successfully.");
                                });
                    }).subscribe();
                    */

            new MaterialDialog.Builder(this)
                    .title(R.string.stops_add_stop)
                    .content(R.string.stops_add_stop_content)
                    .inputType(InputType.TYPE_CLASS_NUMBER)
                    .input(R.string.stops_input_hint, 0, (dialog, input) -> {
                        // Add the stop
                        Timber.d("Add stop %s", input);

                        Snackbar.make(view, "Adding Stop " + input, Snackbar.LENGTH_LONG)
                                .setAction("Action", null)
                                .show();

                        RxPaper.book()
                                .read(Constants.KEY_FAVOURITE_STOPS, new ArrayList<Stop>())
                                .compose(bindToLifecycle())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(stops -> {

                                            Observable.from(Arrays.asList(input.toString()))
                                                    .observeOn(Schedulers.io())
                                                    .subscribeOn(AndroidSchedulers.mainThread())
                                                    .concatMap(s -> mApi.getStop(s))
                                                    .doOnNext(stop -> stops.add(Stop.fromNetModel(stop)))
                                                    .doOnCompleted(() -> {
                                                        RxPaper.book()
                                                                .write(Constants.KEY_FAVOURITE_STOPS, stops)
                                                                .subscribe(success -> {
                                                                    mBus.post(new FavouriteStopsChangedEvent());
                                                                });
                                                    }).subscribe();
                                        },
                                        error -> {

                                        });
                    }).show();


        });

        /* Don't need a drawer yet
        DrawerLayout drawer = mBinding.drawerLayout;
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = mBinding.navView;
        navigationView.setNavigationItemSelectedListener(this);
        */

        ViewPager viewPager = mBinding.appBar.content.viewpager;
        setupViewPager(viewPager);

        mBinding.appBar.tabs.setupWithViewPager(mBinding.appBar.content.viewpager);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = mBinding.drawerLayout;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
        }

        return super.onOptionsItemSelected(item);
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

        DrawerLayout drawer = mBinding.drawerLayout;
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(StopFragment.newInstance(), getString(R.string.tab_stops));
        //adapter.addFragment(RouteFragment.newInstance(null, null), getString(R.string.tab_routes));
        //adapter.addFragment(MapFragment.newInstance(null, null), getString(R.string.tab_map));
        viewPager.setAdapter(adapter);
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
