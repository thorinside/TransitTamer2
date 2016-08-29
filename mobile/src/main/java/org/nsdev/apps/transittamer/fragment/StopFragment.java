package org.nsdev.apps.transittamer.fragment;


import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
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
import com.squareup.otto.Subscribe;
import com.trello.rxlifecycle.components.support.RxFragment;

import org.nsdev.apps.transittamer.App;
import org.nsdev.apps.transittamer.R;
import org.nsdev.apps.transittamer.databinding.FragmentStopBinding;
import org.nsdev.apps.transittamer.databinding.ItemStopBinding;
import org.nsdev.apps.transittamer.events.StopDataChangedEvent;
import org.nsdev.apps.transittamer.managers.DataManager;
import org.nsdev.apps.transittamer.model.FavouriteStops;
import org.nsdev.apps.transittamer.net.model.Stop;
import org.nsdev.apps.transittamer.ui.BindingAdapter;
import org.nsdev.apps.transittamer.utils.OneMinuteTimer;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StopFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StopFragment extends RxFragment {
    private FragmentStopBinding mBinding;
    private BindingAdapter<ItemStopBinding> mAdapter;
    private RealmList<Stop> mStops;

    @Inject
    DataManager mDataManager;

    @Inject
    Bus mBus;

    @Inject
    Realm mRealm;
    private OneMinuteTimer mTimer;
    private Handler mHandler;
    private Runnable mChangeHandler;

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

        LinearLayoutManager layout = new LinearLayoutManager(getActivity());
        mBinding.recyclerView.setLayoutManager(layout);

        mStops = new RealmList<>();

        mAdapter = new BindingAdapter<ItemStopBinding>(R.layout.item_stop) {
            @Override
            public int getItemCount() {
                return mStops.size();
            }

            @Override
            protected void updateBinding(ItemStopBinding binding, int position) {
                Stop stop = mStops.get(position);
                binding.setStop(stop);

                binding.map.onCreate(null);
                binding.map.getMapAsync(googleMap -> setupMap(stop, googleMap));

                binding.setRoutes(stop.getStopRoutes());
                binding.setNext(stop.getNextBus());
            }

            @Override
            protected void recycleBinding(ItemStopBinding binding) {
                Log.e("StopFragment", "Binding Recycling");
            }
        };

        mBinding.recyclerView.setAdapter(mAdapter);

        mTimer = new OneMinuteTimer();
        mTimer.onCreate(this::updateNextBus);

        mHandler = new Handler();
        mChangeHandler = () -> {
            mAdapter.notifyItemRangeChanged(0, mStops.size() - 1);
        };

        // Update the display only if there is a realm
        // change and quiet for at least two seconds afterward
        // to avoid many repaints
        mRealm.addChangeListener((v) -> {
            mHandler.removeCallbacks(mChangeHandler);
            mHandler.postDelayed(mChangeHandler, 2000);
        });

        return mBinding.getRoot();
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

        FavouriteStops favouriteStops = mRealm.where(FavouriteStops.class).findFirst();
        mStops = favouriteStops.getStops();

        for (Stop stop : mStops) {
            mDataManager.syncStop(stop);
        }

        mAdapter.notifyDataSetChanged();

        favouriteStops.addChangeListener(element -> {
            mStops = favouriteStops.getStops();
            mAdapter.notifyDataSetChanged();
            for (Stop stop : mStops) {
                mDataManager.syncStop(stop);
            }
        });

        super.onViewStateRestored(savedInstanceState);
    }

    @Subscribe
    public void onEvent(StopDataChangedEvent event) {
        Log.e("StopFragment", "Got StopDataChangedEvent");
        if (mAdapter != null) {
            mAdapter.notifyItemRangeChanged(0, mStops.size() - 1);
        }
    }
}
