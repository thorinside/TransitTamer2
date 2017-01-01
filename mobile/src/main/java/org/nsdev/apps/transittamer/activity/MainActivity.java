package org.nsdev.apps.transittamer.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
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
import org.nsdev.apps.transittamer.net.TransitTamerAPI;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;
import timber.log.Timber;

public class MainActivity extends RxAppCompatActivity
        implements StopFragment.CoordinatorProvider {

    private static final int REQUEST_INVITE = 1001;

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

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);

        super.onCreate(savedInstanceState);

        ((App) getApplication()).getUserComponent().inject(this);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        Toolbar toolbar = mBinding.toolbar;
        setSupportActionBar(toolbar);

        mViewPager = mBinding.viewpager;
        setupViewPager(mViewPager);
        setupBottomNavigation();

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    private void setupBottomNavigation() {

        BottomBar bottomNavigation = mBinding.bottomNavigation;

        bottomNavigation.setOnTabSelectListener(tabId -> {
            switch (tabId) {
                case R.id.tab_stops:
                    mBinding.viewpager.setCurrentItem(0, false);
                    break;
                case R.id.tab_route:
                    mBinding.viewpager.setCurrentItem(1, false);
                    break;
                case R.id.tab_map:
                    mBinding.viewpager.setCurrentItem(2, false);
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

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(StopFragment.newInstance(), "Stop");
        adapter.addFragment(RouteFragment.newInstance(null, null), "Route");
        adapter.addFragment(MapFragment.newInstance(), "Map");
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_invite:
                onInviteClicked();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onInviteClicked() {
        try {
            Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.action_invite_title))
                    .setMessage(getString(R.string.invitation_message))
                    .setDeepLink(Uri.parse(getString(R.string.invitation_deep_link)))
                    .setCustomImage(Uri.parse(getString(R.string.invitation_custom_image)))
                    .setCallToActionText(getString(R.string.invitation_cta))
                    .build();
            startActivityForResult(intent, REQUEST_INVITE);
        } catch (Throwable ex) {
            FirebaseCrash.report(ex);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkService();
    }

    private void checkService() {
        mDataManager.syncService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Timber.d("onActivityResult: requestCode=%d, resultCode=%d", requestCode, resultCode);

        if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Get the invitation IDs of all sent messages
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                for (String id : ids) {
                    Timber.d("onActivityResult: sent invitation %s", id);
                }
            } else {
                // Sending failed or it was canceled, show failure message to the user
                // ...
            }
        }
    }

}
