package org.nsdev.apps.transittamer.fragment;


import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.transition.TransitionManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Bus;
import com.trello.rxlifecycle.components.support.RxFragment;

import org.nsdev.apps.transittamer.App;
import org.nsdev.apps.transittamer.R;
import org.nsdev.apps.transittamer.databinding.FragmentStopBinding;
import org.nsdev.apps.transittamer.databinding.ItemStopBinding;
import org.nsdev.apps.transittamer.managers.DataManager;
import org.nsdev.apps.transittamer.model.FavouriteStops;
import org.nsdev.apps.transittamer.net.model.Stop;
import org.nsdev.apps.transittamer.ui.BindingAdapter;
import org.nsdev.apps.transittamer.utils.OneMinuteTimer;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmList;
import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StopFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StopFragment extends RxFragment {
    public static final int DELAY_MILLIS = 1000;
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

        RecyclerView.ItemAnimator animator = mBinding.recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        mAdapter = new BindingAdapter<ItemStopBinding>(R.layout.item_stop) {
            @Override
            public int getItemCount() {
                return mStops.size();
            }

            @Override
            protected void updateBinding(ItemStopBinding binding, int position) {
                Stop stop = mStops.get(position);
                binding.setStop(stop);

                stop.removeChangeListeners();
                stop.addChangeListener(element -> {
                    Timber.d("Stop element changed: %s", element);
                    binding.setRoutes(stop.getStopRoutes());
                    binding.setNext(stop.getNextBus());
                });

                binding.map.onCreate(null);
                binding.map.getMapAsync(googleMap -> setupMap(stop, googleMap));

                binding.setRoutes(stop.getStopRoutes());
                binding.setNext(stop.getNextBus());
            }

            @Override
            protected void recycleBinding(ItemStopBinding binding) {
                Log.e("StopFragment", "Binding Recycling");
                binding.getStop().removeChangeListeners();
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

                        Snackbar.make(((CoordinatorProvider) getActivity()).getCoordinator(), "Removed stop #" + stop.getStop_id(), Snackbar.LENGTH_LONG).setAction(R.string.undo,
                                view -> {
                                    mRealm.executeTransaction(realm2 -> {
                                        favouriteStops.getStops().add(stop);
                                        favouriteStops.setLastUpdated(new Date());
                                        mAdapter.notifyItemInserted(position);
                                    });
                                }).show();
                    }
                });
            }
        });

        itemTouchHelper.attachToRecyclerView(mBinding.recyclerView);
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
}
