package org.nsdev.apps.transittamer.fragment;

import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import org.nsdev.apps.transittamer.App;
import org.nsdev.apps.transittamer.R;
import org.nsdev.apps.transittamer.databinding.FragmentMapBinding;
import org.nsdev.apps.transittamer.model.FavouriteStops;
import org.nsdev.apps.transittamer.net.model.Stop;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmResults;
import timber.log.Timber;

/**
 * A {@link Fragment} subclass that shows a transit map to the user.
 */
public class MapFragment extends Fragment {

    private GoogleMap mMap;
    private FragmentMapBinding mBinding;

    @Inject
    Realm mRealm;
    private RealmResults<FavouriteStops> mFavouriteStops;

    public MapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MapFragment.
     */
    public static MapFragment newInstance() {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //noinspection StatementWithEmptyBody
        if (getArguments() != null) {
        }

        ((App) getContext().getApplicationContext()).getUserComponent().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_map, container, false);

        mBinding.map.onCreate(savedInstanceState);
        mBinding.map.getMapAsync(this::onMapReady);

        return mBinding.getRoot();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding.map.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        mBinding.map.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mBinding.map.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mBinding.map.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mBinding.map.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mBinding.map.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mBinding.map.onLowMemory();
    }

    private void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        try {
            // Customize the map
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.map_theme));

            if (!success) {
                Timber.e("Style Parsing Failed");
            }
        } catch (Resources.NotFoundException e) {
            Timber.e(e, "Can't find style");
        }

        // Display calgary and surrounding area by default
        CameraUpdate calgaryUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(51.0486, -114.0708), 11);
        mMap.moveCamera(calgaryUpdate);

        // Display all of the user's favourite stops
        mFavouriteStops = mRealm.where(FavouriteStops.class).equalTo("id", 0).findAllAsync();

        mFavouriteStops.addChangeListener(stops -> {
            Timber.d("Favourite stops changed.");
            mMap.clear();
            for (Stop stop : stops.get(0).getStops()) {
                LatLng latLng = new LatLng(stop.getStop_lat(), stop.getStop_lon());
                BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_bus_stop);
                mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .icon(icon)
                        .flat(true)
                        .anchor(0.5f, 0.5f)
                );

            }
        });

    }
}
