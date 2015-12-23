package org.nsdev.apps.transittamer.fragment;


import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cesarferreira.rxpaper.RxPaper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.trello.rxlifecycle.components.support.RxFragment;

import org.nsdev.apps.transittamer.Constants;
import org.nsdev.apps.transittamer.R;
import org.nsdev.apps.transittamer.databinding.FragmentStopBinding;
import org.nsdev.apps.transittamer.databinding.ItemStopBinding;
import org.nsdev.apps.transittamer.model.Stop;
import org.nsdev.apps.transittamer.ui.BindingAdapter;

import java.util.ArrayList;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StopFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StopFragment extends RxFragment {
    private FragmentStopBinding mBinding;
    private BindingAdapter<ItemStopBinding> mAdapter;
    private ArrayList<Stop> mStops;


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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_stop, container, false);

        LinearLayoutManager layout = new LinearLayoutManager(getActivity());
        mBinding.recyclerView.setLayoutManager(layout);

        mStops = new ArrayList<>();

        mAdapter = new BindingAdapter<ItemStopBinding>(R.layout.item_stop) {
            @Override
            public int getItemCount() {
                return mStops.size();
            }

            @Override
            protected void updateBinding(ItemStopBinding binding, int position) {
                binding.setStop(mStops.get(position));
                binding.map.onCreate(null);
                binding.map.getMapAsync(googleMap -> {

                    LatLng latLng = new LatLng(binding.getStop().getLatitude(), binding.getStop().getLongitude());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

                    // Doesn't support vector drawables for whatever reason
                    BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.mipmap.ic_stop);
                    googleMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .icon(icon)
                            .anchor(0.5f, 0.5f)
                    );

                    googleMap.getUiSettings().setMapToolbarEnabled(false);
                });

            }
        };

        mBinding.recyclerView.setAdapter(mAdapter);

        return mBinding.getRoot();
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
                    mAdapter.notifyDataSetChanged();
                });


        super.onViewStateRestored(savedInstanceState);
    }
}
