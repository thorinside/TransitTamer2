package org.nsdev.apps.transittamer.fragment;


import android.Manifest;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.transition.TransitionManager;
import android.support.v4.app.Fragment;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Bus;
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
import org.nsdev.apps.transittamer.model.StopRouteSchedule;
import org.nsdev.apps.transittamer.model.StopViewModel;
import org.nsdev.apps.transittamer.net.model.Route;
import org.nsdev.apps.transittamer.net.model.Stop;
import org.nsdev.apps.transittamer.net.model.StopTime;
import org.nsdev.apps.transittamer.net.model.Trip;
import org.nsdev.apps.transittamer.ui.BindingAdapter;
import org.nsdev.apps.transittamer.ui.StartLinearSnapHelper;
import org.nsdev.apps.transittamer.utils.OneMinuteTimer;
import org.nsdev.apps.transittamer.utils.ScheduleUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmList;
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
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
    private BindingAdapter<ItemStopBinding> mAdapter;
    private List<Stop> mStops;

    public interface CoordinatorProvider {
        View getCoordinator();
    }

    @Inject
    DataManager mDataManager;

    @Inject
    Bus mBus;

    @Inject
    Realm mRealm;

    @Inject
    ReactiveLocationProvider mLocationProvider;

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

        mBus.register(this);

        mStops = new RealmList<>();

        setupRecyclerView();

        mTimer = new OneMinuteTimer();
        mTimer.onCreate(this::updateNextBus);

        mHandler = new Handler();
        mChangeHandler = () -> {
            checkEmpty();
        };

        return mBinding.getRoot();
    }

    private void setupRecyclerView() {
        LinearLayoutManager layout = new LinearLayoutManager(getActivity());
        mBinding.recyclerView.setLayoutManager(layout);

        mAdapter = new BindingAdapter<ItemStopBinding>(R.layout.item_stop) {
            @Override
            public int getItemCount() {
                return mStops.size();
            }

            @Override
            protected void updateBinding(ItemStopBinding binding, int position) {
                Stop stop = mStops.get(position);
                binding.setViewModel(new StopViewModel(stop, view -> {
                    binding.getViewModel().setOpen(!binding.getViewModel().isOpen());
                    stopClicked(mStops.get(position));
                }));

                stop.removeChangeListeners();
                stop.addChangeListener(element -> {
                    Timber.d("Stop element changed: %s", element);
                    StopViewModel viewModel = binding.getViewModel();
                    viewModel.notifyPropertyChanged(BR.routes);
                    viewModel.notifyPropertyChanged(BR.next);
                    RecyclerView.Adapter adapter = binding.routeDetailList.getAdapter();
                    if (adapter != null) {
                        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
                    }
                });

                binding.map.onCreate(null);
                binding.map.getMapAsync(googleMap -> setupMap(stop, googleMap));

                RecyclerView routeDetailList = binding.routeDetailList;
                if (routeDetailList.getLayoutManager() == null) {
                    routeDetailList.setHasFixedSize(true);
                    routeDetailList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
                }
                routeDetailList.setAdapter(new BindingAdapter<ItemStopDetailBinding>(R.layout.item_stop_detail) {

                    @Override
                    public int getItemCount() {
                        return stop.getSchedules().size();
                    }

                    @Override
                    protected void updateBinding(ItemStopDetailBinding detailBinding, int position) {
                        StopRouteSchedule stopRouteSchedule = stop.getSchedules().get(position);

                        Route route = stopRouteSchedule.getRoute();
                        detailBinding.setRoute(String.format("%s %s", route.getRoute_short_name(), route.getRoute_long_name()));

                        RecyclerView stopTimesList = detailBinding.stopTimesList;
                        if (stopTimesList.getLayoutManager() == null) {
                            final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                            stopTimesList.setHasFixedSize(true);
                            SnapHelper helper = new StartLinearSnapHelper();
                            helper.attachToRecyclerView(stopTimesList);
                            stopTimesList.setLayoutManager(linearLayoutManager);
                        }
                        stopTimesList.setAdapter(new BindingAdapter<ItemStopTimesBinding>(R.layout.item_stop_times) {
                            @Override
                            public int getItemCount() {
                                return stopRouteSchedule.getSchedule().size();
                            }

                            @Override
                            protected void updateBinding(ItemStopTimesBinding timesBinding, int position) {
                                StopTime stopTime = stopRouteSchedule.getSchedule().get(position);
                                String time = ScheduleUtils.getTimeString(getContext(), stopTime);
                                timesBinding.setTime(time);
                                timesBinding.setBold(ScheduleUtils.getIndexOfNext(stopRouteSchedule) == position);
                            }

                            @Override
                            protected void recycleBinding(ItemStopTimesBinding binding) {

                            }

                        });

                        int indexOfNext = ScheduleUtils.getIndexOfNext(stopRouteSchedule);
                        ((LinearLayoutManager) stopTimesList.getLayoutManager()).scrollToPositionWithOffset(indexOfNext, 0);
                    }

                    @Override
                    protected void recycleBinding(ItemStopDetailBinding binding) {

                    }
                });
            }

            @Override
            protected void recycleBinding(ItemStopBinding binding) {
                Log.e("StopFragment", "Binding Recycling");
                binding.getViewModel().getStop().removeChangeListeners();
            }
        };

        mBinding.recyclerView.setAdapter(mAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Timber.d("Swiped item %d", position);
                FavouriteStops favouriteStops = mRealm.where(FavouriteStops.class).findFirst();
                mRealm.executeTransaction(realm -> {
                    Stop stop = favouriteStops.getStops().get(position);
                    favouriteStops.getStops().remove(stop);
                    favouriteStops.setLastUpdated(new Date());
                    mAdapter.notifyItemRemoved(position);

                    if (getActivity() instanceof CoordinatorProvider) {

                        Snackbar.make(((CoordinatorProvider) getActivity()).getCoordinator(), "Removed stop #" + stop.getStop_id(), Snackbar.LENGTH_LONG)
                                .setAction(R.string.undo, view -> {
                                    mRealm.executeTransaction(realm2 -> {
                                        favouriteStops.getStops().add(position, stop);
                                        favouriteStops.setLastUpdated(new Date());
                                    });
                                    mAdapter.notifyItemInserted(position);
                                }).show();
                    }
                });
            }
        });

        //itemTouchHelper.attachToRecyclerView(mBinding.recyclerView);
    }

    private void stopClicked(Stop stop) {
        Timber.d("Stop clicked: %s", stop.getStop_id());

        for (StopRouteSchedule stopRouteSchedule : stop.getSchedules()) {
            Timber.d("Route: %s %s", stopRouteSchedule.getRoute().getRoute_short_name(), stopRouteSchedule.getRoute().getRoute_long_name());
            for (StopTime stopTime : stopRouteSchedule.getSchedule()) {
                String tripId = stopTime.getTrip_id();
                Trip trip = mRealm.where(Trip.class)
                        .equalTo("trip_id", tripId)
                        .findFirst();
                Timber.d(" StopTime: %s %s", stopTime.getDeparture_time(), trip.getTrip_headsign());
            }
        }
    }

    private void updateNextBus() {
        for (Stop stop : mStops) {
            mDataManager.syncStopNextBus(stop);
        }
    }

    private void setupMap(Stop stop, GoogleMap googleMap) {
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBus.unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRealm.close();
        mTimer.onDestroy();
        mHandler.removeCallbacks(mChangeHandler);
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

        mFavouriteStops = mRealm.where(FavouriteStops.class).findFirst();
        mStops = mFavouriteStops.getStops();

        checkEmpty();

        for (Stop stop : mStops) {
            mDataManager.syncStop(stop);
        }

        mFavouriteStops.addChangeListener(element -> {
            mHandler.removeCallbacks(mChangeHandler);
            mHandler.postDelayed(mChangeHandler, DELAY_MILLIS);
        });

        super.onViewStateRestored(savedInstanceState);
    }

    private void checkEmpty() {

        TransitionManager.beginDelayedTransition((ViewGroup) mBinding.getRoot());

        if (mFavouriteStops == null || mStops == null || mStops.size() == 0) {
            mBinding.setEmpty(true);
        } else {
            mBinding.setEmpty(false);
        }
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
        sortedStops.addAll(mStops);

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

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MyDiffCallback(sortedStops, mStops), true);
        diffResult.dispatchUpdatesTo(mAdapter);
        mStops = sortedStops;

        mBinding.recyclerView.scrollToPosition(0);
    }

    public class MyDiffCallback extends DiffUtil.Callback {

        List<Stop> old;
        List<Stop> newer;

        public MyDiffCallback(List<Stop> newStops, List<Stop> oldStops) {
            old = oldStops;
            newer = newStops;
        }

        @Override
        public int getOldListSize() {
            return old.size();
        }

        @Override
        public int getNewListSize() {
            return newer.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return old.get(oldItemPosition).getStop_id().equals(newer.get(newItemPosition).getStop_id());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return old.get(oldItemPosition).equals(newer.get(newItemPosition));
        }
    }
}
