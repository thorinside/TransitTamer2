package org.nsdev.apps.transittamer.fragment;


import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cesarferreira.rxpaper.RxPaper;
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
import org.nsdev.apps.transittamer.Constants;
import org.nsdev.apps.transittamer.R;
import org.nsdev.apps.transittamer.databinding.FragmentStopBinding;
import org.nsdev.apps.transittamer.databinding.ItemStopBinding;
import org.nsdev.apps.transittamer.events.StopDataChangedEvent;
import org.nsdev.apps.transittamer.managers.DataManager;
import org.nsdev.apps.transittamer.model.Stop;
import org.nsdev.apps.transittamer.ui.BindingAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StopFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StopFragment extends RxFragment {
    private FragmentStopBinding mBinding;
    private BindingAdapter<ItemStopBinding> mAdapter;
    private ArrayList<Stop> mStops;

    @Inject
    DataManager mDataManager;

    @Inject
    Bus mBus;

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

        mStops = new ArrayList<>();

        mAdapter = new BindingAdapter<ItemStopBinding>(R.layout.item_stop) {
            @Override
            public int getItemCount() {
                return mStops.size();
            }

            Map<ItemStopBinding, CompositeSubscription> bindingSubscriptions = new HashMap<>();

            @Override
            protected void updateBinding(ItemStopBinding binding, int position) {
                if (bindingSubscriptions.containsKey(binding)) {
                    bindingSubscriptions.remove(binding).unsubscribe();
                }
                Stop stop = mStops.get(position);
                binding.setStop(stop);

                binding.map.onCreate(null);
                binding.map.getMapAsync(googleMap -> setupMap(stop, googleMap));

                Subscription subscription = mDataManager.getStopRoutes(stop).subscribe(binding::setRoutes, error -> {
                });
                Subscription subscription1 = mDataManager.getNextBus(stop).subscribe(binding::setNext, error -> {
                });

                CompositeSubscription compositeSubscription = new CompositeSubscription(subscription, subscription1);
                bindingSubscriptions.put(binding, compositeSubscription);
            }

            @Override
            protected void recycleBinding(ItemStopBinding binding) {
                Log.e("StopFragment", "Binding Recycling");
                CompositeSubscription compositeSubscription = bindingSubscriptions.remove(binding);
                if (compositeSubscription != null) {
                    compositeSubscription.unsubscribe();
                }
            }
        };

        mBinding.recyclerView.setAdapter(mAdapter);

        return mBinding.getRoot();
    }

    private void setupMap(Stop stop, GoogleMap googleMap) {
        LatLng latLng = new LatLng(stop.getLatitude(), stop.getLongitude());
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
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {

        RxPaper.with(getContext())
                .read(Constants.KEY_FAVOURITE_STOPS, new ArrayList<Stop>())
                .compose(bindToLifecycle())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stops -> {
                    mStops = (ArrayList<Stop>) stops;
                    for (Stop stop : mStops) {
                        mDataManager.syncStop(stop);
                    }
                    mAdapter.notifyDataSetChanged();
                });


        super.onViewStateRestored(savedInstanceState);
    }

    @Subscribe
    public void onEvent(StopDataChangedEvent event) {
        Log.e("StopFragment", "Got StopDataChangedEvent");
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }
}
