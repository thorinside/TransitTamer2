package org.nsdev.apps.transittamer.fragment;

import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.nsdev.apps.transittamer.App;
import org.nsdev.apps.transittamer.R;
import org.nsdev.apps.transittamer.databinding.FragmentMapBinding;
import org.nsdev.apps.transittamer.model.FavouriteStops;
import org.nsdev.apps.transittamer.model.StopDetailModel;
import org.nsdev.apps.transittamer.model.StopRouteSchedule;
import org.nsdev.apps.transittamer.net.TransitTamerAPI;
import org.nsdev.apps.transittamer.net.model.ShapePath;
import org.nsdev.apps.transittamer.net.model.Stop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmResults;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * A {@link Fragment} subclass that shows a transit map to the user.
 */
public class MapFragment extends Fragment {

    private GoogleMap mMap;
    private FragmentMapBinding mBinding;

    @Inject
    Realm mRealm;

    @Inject
    TransitTamerAPI mApi;

    private RealmResults<FavouriteStops> mFavouriteStops;
    private HashMap<Stop, Marker> mMarkerForStop = new HashMap<>();

    private List<Polyline> mRouteShapes = new ArrayList<>();

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
        mMap.setOnMarkerClickListener(marker -> {

            Stop stop = (Stop) marker.getTag();

            for (Polyline routeShape : mRouteShapes) {
                routeShape.remove();
            }
            mRouteShapes.clear();

            StopDetailModel model = new StopDetailModel(mRealm, stop);
            for (StopRouteSchedule schedule : model.getSchedules()) {
                String routeId = schedule.getRoute().getRoute_id();

                mApi.getShape(routeId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(shapePaths -> {
                                    for (ShapePath shapePath : shapePaths) {
                                        List<LatLng> shape = PolyUtil.decode(shapePath.getPath());
                                        PolylineOptions line = new PolylineOptions()
                                                .addAll(shape)
                                                .width(10)
                                                .clickable(false)
                                                .visible(true)
                                                .zIndex(1)
                                                .color(Color.BLUE);
                                        Polyline polyline = mMap.addPolyline(line);
                                        polyline.setVisible(true);
                                        mRouteShapes.add(polyline);
                                    }
                                },
                                error -> {
                                    Timber.e(error);
                                });

            }

            return false;
        });


        // Display all of the user's favourite stops
        mFavouriteStops = mRealm.where(FavouriteStops.class).equalTo("id", 0).findAllAsync();

        mFavouriteStops.addChangeListener(stops -> {
            Timber.d("Favourite stops changed.");

            Set<Stop> currentStops = new HashSet<>();
            currentStops.addAll(mMarkerForStop.keySet());

            for (Stop stop : stops.get(0).getStops()) {
                LatLng latLng = new LatLng(stop.getStop_lat(), stop.getStop_lon());
                BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_bus_stop);

                currentStops.remove(stop);

                if (mMarkerForStop.containsKey(stop)) {
                    Marker marker = mMarkerForStop.get(stop);
                    marker.setTag(stop);
                    marker.setTitle(String.format("%s: %s", stop.getStop_code(), stop.getStop_name()));
                    marker.setSnippet(stop.getNextBus());

                    // Make sure to update the info window contents if shown.
                    // This doesn't seem to flicker, and updates the text.
                    if (marker.isInfoWindowShown()) {
                        marker.hideInfoWindow();
                        marker.showInfoWindow();
                    }
                } else {
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title(String.format("%s: %s", stop.getStop_code(), stop.getStop_name()))
                            .snippet(stop.getNextBus())
                            .infoWindowAnchor(0.5f, 0.0f)
                            .icon(icon)
                            .flat(true)
                            .zIndex(2)
                            .anchor(0.5f, 0.5f)
                    );
                    marker.setTag(stop);
                    mMarkerForStop.put(stop, marker);
                }
            }

            // Remove the remaining stops, since it's no longer in the
            // favourites.
            for (Stop stop : currentStops) {
                Marker marker = mMarkerForStop.get(stop);
                marker.remove();
                mMarkerForStop.remove(stop);
            }
        });

    }
}
