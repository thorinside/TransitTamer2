package org.nsdev.apps.transittamer.fragment;


import android.Manifest;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.transition.TransitionManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.genius.groupie.GroupAdapter;
import com.genius.groupie.Item;
import com.genius.groupie.UpdatingGroup;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.trello.rxlifecycle.components.support.RxFragment;

import org.nsdev.apps.transittamer.App;
import org.nsdev.apps.transittamer.BR;
import org.nsdev.apps.transittamer.R;
import org.nsdev.apps.transittamer.databinding.FragmentStopBinding;
import org.nsdev.apps.transittamer.databinding.ItemStopBinding;
import org.nsdev.apps.transittamer.databinding.ItemStopDetailBinding;
import org.nsdev.apps.transittamer.databinding.ItemStopTimesBinding;
import org.nsdev.apps.transittamer.managers.DataManager;
import org.nsdev.apps.transittamer.model.FavouriteStops;
import org.nsdev.apps.transittamer.model.StopDetailModel;
import org.nsdev.apps.transittamer.model.StopRouteSchedule;
import org.nsdev.apps.transittamer.model.StopViewModel;
import org.nsdev.apps.transittamer.net.TransitTamerAPI;
import org.nsdev.apps.transittamer.net.model.Route;
import org.nsdev.apps.transittamer.net.model.Stop;
import org.nsdev.apps.transittamer.net.model.StopTime;
import org.nsdev.apps.transittamer.ui.BindingAdapter;
import org.nsdev.apps.transittamer.ui.StartLinearSnapHelper;
import org.nsdev.apps.transittamer.utils.OneMinuteTimer;
import org.nsdev.apps.transittamer.utils.ScheduleUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmList;
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StopFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StopFragment extends RxFragment {
    public static final int DELAY_MILLIS = 1000;
    private static final float SUFFICIENT_ACCURACY = 50.0f;
    private static final long LOCATION_TIMEOUT_IN_SECONDS = 30;
    private static final long LOCATION_UPDATE_INTERVAL = 5000;
    private FragmentStopBinding mBinding;
    private UpdatingGroup mUpdatingGroup;
    private GroupAdapter mAdapter;

    public interface CoordinatorProvider {
        View getCoordinator();
    }

    @Inject
    DataManager mDataManager;

    @Inject
    Realm mRealm;

    @Inject
    ReactiveLocationProvider mLocationProvider;

    @Inject
    TransitTamerAPI mApi;

    HashMap<Integer, StopViewModel> mViewModelForPosition = new HashMap<>();
    List<GoogleMap> mMaps = new ArrayList<>();

    private OneMinuteTimer mTimer;
    private Handler mHandler;
    private Runnable mChangeHandler;
    private FavouriteStops mFavouriteStops;

    public StopFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StopFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static StopFragment newInstance() {
        StopFragment fragment = new StopFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((App) getContext().getApplicationContext()).getUserComponent().inject(this);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_stop, container, false);

        setupRecyclerView();

        mTimer = new OneMinuteTimer();
        mTimer.onCreate(this::updateNextBus);

        mHandler = new Handler();
        mChangeHandler = () -> {
            updateStopList();
        };

        return mBinding.getRoot();
    }

    public class StopItem extends Item<ItemStopBinding> {
        private final Stop mStop;
        private StopViewModel mViewModel;

        public StopItem(Stop stop) {
            mStop = stop;
        }

        @Override
        public void bind(ItemStopBinding viewBinding, int position) {
            mViewModel = new StopViewModel(mStop);
            viewBinding.setViewModel(mViewModel);

            mStop.removeChangeListeners();
            mStop.addChangeListener(element -> {
                Timber.d("Stop element changed: %s", element);
                StopViewModel viewModel = viewBinding.getViewModel();
                viewModel.notifyPropertyChanged(BR.routes);
                viewModel.notifyPropertyChanged(BR.next);
                RecyclerView.Adapter adapter = viewBinding.routeDetailList.getAdapter();
                if (adapter != null) {
                    adapter.notifyItemRangeChanged(0, adapter.getItemCount());
                }
            });

            viewBinding.map.onCreate(null);
            viewBinding.map.getMapAsync(googleMap -> setupMap(mStop, viewBinding.map, googleMap));
            Menu menu = viewBinding.toolbar.getMenu();
            if (menu != null) {
                menu.clear();
            }
            viewBinding.toolbar.inflateMenu(R.menu.stop_card);
            viewBinding.toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_delete) {
                    deleteStop(mStop);
                    return true;
                }
                return false;
            });

            RecyclerView routeDetailList = viewBinding.routeDetailList;
            if (routeDetailList.getLayoutManager() == null) {
                routeDetailList.setHasFixedSize(true);
                routeDetailList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
            }

            StopDetailModel stopDetailModel = new StopDetailModel(mRealm, mStop);

            routeDetailList.setAdapter(new StopDetailAdapter(getContext(), mRealm, stopDetailModel));
        }

        @Override
        public int getLayout() {
            return R.layout.item_stop;
        }

        @Override
        public long getId() {
            return Long.parseLong(mStop.getStop_id());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof StopItem && mStop.getStop_id().equals(((StopItem) obj).mStop.getStop_id());
        }

        public StopViewModel getViewModel() {
            return mViewModel;
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layout = new LinearLayoutManager(getActivity());
        mBinding.recyclerView.setLayoutManager(layout);

        mAdapter = new GroupAdapter();

        mAdapter.setOnItemClickListener((item, view) -> {
            StopItem stopItem = (StopItem) item;
            StopViewModel viewModel = stopItem.getViewModel();
            viewModel.setOpen(!viewModel.isOpen());
        });

        mUpdatingGroup = new UpdatingGroup();
        mAdapter.add(mUpdatingGroup);

        mBinding.recyclerView.setAdapter(mAdapter);
    }

    private void updateNextBus() {
        if (mFavouriteStops != null && mFavouriteStops.isValid() && mFavouriteStops.isLoaded() && mFavouriteStops.getStops() != null) {
            for (Stop stop : mFavouriteStops.getStops()) {
                mDataManager.syncStopNextBus(stop);
            }
        }
    }

    private void setupMap(Stop stop, MapView map, GoogleMap googleMap) {
        MapsInitializer.initialize(getContext().getApplicationContext());

        map.setTag(googleMap);

        LatLng latLng = new LatLng(stop.getStop_lat(), stop.getStop_lon());
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        // Doesn't support vector drawables for whatever reason
        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_bus_stop);
        googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(icon)
                .flat(true)
                .anchor(0.5f, 0.5f)
        );

        googleMap.getUiSettings().setMapToolbarEnabled(false);

        mMaps.add(googleMap);
    }

    @Override
    public void onDestroyView() {
        Timber.w("onDestroyView");
        super.onDestroyView();

        for (GoogleMap map : mMaps) {
            map.clear();
            map.setMapType(GoogleMap.MAP_TYPE_NONE);
        }

        for (Stop stop : mFavouriteStops.getStops()) {
            stop.removeChangeListeners();
        }

        mAdapter.notifyDataSetChanged();
        mBinding.recyclerView.setAdapter(null);
        mHandler.removeCallbacks(mChangeHandler);
        mTimer.onDestroy();
    }

    @Override
    public void onDestroy() {
        Timber.w("onDestroy");
        super.onDestroy();
        mRealm.close();
    }

    @Override
    public void onPause() {
        super.onPause();
        mTimer.onPause();
        mHandler.removeCallbacks(mChangeHandler);
    }

    @Override
    public void onResume() {
        super.onResume();
        mTimer.onResume();
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {

        mFavouriteStops = mRealm.where(FavouriteStops.class).findFirstAsync();

        mFavouriteStops.addChangeListener(element -> {
            mHandler.removeCallbacks(mChangeHandler);
            mHandler.postDelayed(mChangeHandler, DELAY_MILLIS);
        });

        updateNextBus();

        super.onViewStateRestored(savedInstanceState);
    }

    private void updateStopList() {

        TransitionManager.beginDelayedTransition((ViewGroup) mBinding.getRoot());

        if (mFavouriteStops == null || mFavouriteStops.getStops() == null || mFavouriteStops.getStops().size() == 0) {
            mBinding.setEmpty(true);
        } else {
            mBinding.setEmpty(false);
            RealmList<Stop> stops = mFavouriteStops.getStops();
            mUpdatingGroup.update(makeListItems(stops));
        }
    }

    private List<? extends Item> makeListItems(RealmList<Stop> stops) {
        ArrayList<Item> retval = new ArrayList<>();
        for (Stop stop : stops) {
            StopItem item = new StopItem(stop);
            retval.add(item);
        }
        return retval;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.stop, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_stop_sort_near:
                sortStopsByNearest();
                return true;
            case R.id.menu_add_stop:
                doAddStop();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void sortStopsByNearest() {

        RxPermissions.getInstance(getContext())
                .request(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(granted -> {
                    if (granted) {
                        getNearestLocation();
                    }
                });
    }

    private void getNearestLocation() {
        LocationRequest req = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setExpirationDuration(TimeUnit.SECONDS.toMillis(LOCATION_TIMEOUT_IN_SECONDS))
                .setInterval(LOCATION_UPDATE_INTERVAL);

        @SuppressWarnings("MissingPermission")
        Observable<Location> goodEnoughQuicklyOrNothingObservable = mLocationProvider.getUpdatedLocation(req)
                .filter(location -> location.getAccuracy() < SUFFICIENT_ACCURACY)
                .timeout(LOCATION_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS, Observable.just(null), AndroidSchedulers.mainThread())
                .first()
                .observeOn(AndroidSchedulers.mainThread());

        goodEnoughQuicklyOrNothingObservable.subscribe(
                location -> {
                    sortStopsByLocation(location);
                },
                error -> {
                    Timber.e(error, "Error getting location.");
                }
        );
    }

    private void sortStopsByLocation(Location location) {
        if (location == null) {
            Timber.d("No location found.");
            return;
        }
        Timber.d("Sorting Stops By Location: %s", location);

        ArrayList<Stop> sortedStops = new ArrayList<Stop>();
        sortedStops.addAll(mFavouriteStops.getStops());

        Collections.sort(sortedStops, (loc1, loc2) -> {
            float[] results = new float[2];
            Location.distanceBetween(loc1.getStop_lat(), loc1.getStop_lon(), location.getLatitude(), location.getLongitude(), results);

            float[] results2 = new float[2];
            Location.distanceBetween(loc2.getStop_lat(), loc2.getStop_lon(), location.getLatitude(), location.getLongitude(), results2);

            if (results[0] > results2[0]) {
                return 1;
            }
            if (results[0] < results2[0]) {
                return -1;
            }
            return 0;
        });

        mRealm.executeTransaction(realm -> {
            mFavouriteStops.getStops().clear();
            mFavouriteStops.getStops().addAll(sortedStops);
            mFavouriteStops.setLastUpdated(new Date());
            mBinding.recyclerView.scrollToPosition(0);
        });
    }

    private void doAddStop() {
        new MaterialDialog.Builder(getContext())
                .title(R.string.stops_add_stop)
                .content(R.string.stops_add_stop_content)
                .inputType(InputType.TYPE_CLASS_NUMBER)
                .input(R.string.stops_input_hint, 0, (dialog, input) -> {
                    RealmList<Stop> newStops = new RealmList<>();

                    Observable.just(input.toString())
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
                                        Timber.e(error, "Adding stop");
                                    },
                                    () -> {
                                        FavouriteStops favouriteStops = mRealm.where(FavouriteStops.class).findFirst();
                                        mRealm.executeTransaction(realm -> {
                                            favouriteStops.getStops().addAll(newStops);
                                            favouriteStops.setLastUpdated(new Date());
                                        });

                                        mDataManager.syncService(true);

                                    }
                            );
                }).show();
    }

    private void deleteStop(Stop stop) {
        FavouriteStops favouriteStops = mRealm.where(FavouriteStops.class).findFirst();
        int position = favouriteStops.getStops().indexOf(stop);
        mRealm.executeTransaction(realm -> {
            favouriteStops.getStops().remove(stop);
            favouriteStops.setLastUpdated(new Date());

            if (getActivity() instanceof CoordinatorProvider) {

                Snackbar.make(((CoordinatorProvider) getActivity()).getCoordinator(), "Removed stop #" + stop.getStop_id(), Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, view -> {
                            mRealm.executeTransaction(realm2 -> {
                                favouriteStops.getStops().add(position, stop);
                                favouriteStops.setLastUpdated(new Date());
                            });
                        }).show();
            }
        });
    }

    private static class StopDetailAdapter extends BindingAdapter<ItemStopDetailBinding> {

        private final Context mContext;
        private final Realm mRealm;
        private final StopDetailModel mStopDetailModel;

        StopDetailAdapter(Context context, Realm realm, StopDetailModel stopDetailModel) {
            super(R.layout.item_stop_detail);
            mContext = context;
            mRealm = realm;
            mStopDetailModel = stopDetailModel;
        }

        @Override
        public int getItemCount() {
            return mStopDetailModel.getSchedules().size();
        }

        @Override
        protected void updateBinding(ItemStopDetailBinding detailBinding, int position) {
            StopRouteSchedule stopRouteSchedule = mStopDetailModel.getSchedules().get(position);

            Route route = stopRouteSchedule.getRoute();
            detailBinding.setRoute(String.format("%s \u2014 %s", route.getRoute_short_name(), ScheduleUtils.getHeadSign(mRealm, stopRouteSchedule)));

            RecyclerView stopTimesList = detailBinding.stopTimesList;
            if (stopTimesList.getLayoutManager() == null) {
                final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
                stopTimesList.setHasFixedSize(true);
                SnapHelper helper = new StartLinearSnapHelper();
                helper.attachToRecyclerView(stopTimesList);
                stopTimesList.setLayoutManager(linearLayoutManager);
            }
            stopTimesList.setAdapter(new StopTimesAdapter(mContext, stopRouteSchedule));

            int indexOfNext = ScheduleUtils.getIndexOfNext(stopRouteSchedule);
            if (indexOfNext > 0) {
                ((LinearLayoutManager) stopTimesList.getLayoutManager()).scrollToPositionWithOffset(indexOfNext, 0);
            }
        }

        @Override
        protected void recycleBinding(ItemStopDetailBinding binding) {
            Timber.w("recycleBinding-b");
        }
    }

    private static class StopTimesAdapter extends BindingAdapter<ItemStopTimesBinding> {
        private final Context mContext;
        private final StopRouteSchedule mStopRouteSchedule;

        StopTimesAdapter(Context context, StopRouteSchedule stopRouteSchedule) {
            super(R.layout.item_stop_times);
            mContext = context;
            mStopRouteSchedule = stopRouteSchedule;
        }

        @Override
        public int getItemCount() {
            return mStopRouteSchedule.getSchedule().size();
        }

        @Override
        protected void updateBinding(ItemStopTimesBinding timesBinding, int position) {
            StopTime stopTime = mStopRouteSchedule.getSchedule().get(position);
            String time = ScheduleUtils.getTimeString(mContext, stopTime);
            timesBinding.setTime(time);
            timesBinding.setBold(ScheduleUtils.getIndexOfNext(mStopRouteSchedule) == position);
        }

        @Override
        protected void recycleBinding(ItemStopTimesBinding binding) {
            Timber.w("recycleBinding-a");
        }
    }
}
