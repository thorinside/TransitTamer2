package org.nsdev.apps.transittamer.fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.tbruyelle.rxpermissions.RxPermissions;

import org.nsdev.apps.transittamer.App;
import org.nsdev.apps.transittamer.R;
import org.nsdev.apps.transittamer.databinding.FragmentMapBinding;
import org.nsdev.apps.transittamer.model.FavouriteStops;
import org.nsdev.apps.transittamer.model.StopDetailModel;
import org.nsdev.apps.transittamer.net.TransitTamerAPI;
import org.nsdev.apps.transittamer.net.model.ShapePath;
import org.nsdev.apps.transittamer.net.model.Stop;
import org.nsdev.apps.transittamer.utils.ScheduleUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    public static final int[] COLORS = new int[]{Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.MAGENTA, Color.YELLOW};
    private HashMap<Polyline, String> mRouteNames = new HashMap<>();

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
    public void onAttach(Context context) {
        Timber.w("onAttach");
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Timber.w("onCreate");

        super.onCreate(savedInstanceState);
        //noinspection StatementWithEmptyBody
        if (getArguments() != null) {
        }

        ((App) getContext().getApplicationContext()).getUserComponent().inject(this);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.w("onCreateView");
        // Inflate the layout for this fragment
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_map, container, false);

        mBinding.map.onCreate(savedInstanceState);
        mBinding.map.getMapAsync(this::onMapReady);

        return mBinding.getRoot();

    }

    @Override
    public void onDestroyView() {
        Timber.w("onDestroyView");
        super.onDestroyView();

        GoogleMap googleMap = (GoogleMap) mBinding.map.getTag();
        if (googleMap != null) {
            googleMap.clear();
            googleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        }

        mMarkerForStop.clear();
        mRouteShapes.clear();
        mRouteNames.clear();
        mBinding.map.onDestroy();
        mFavouriteStops.removeChangeListeners();
    }

    @Override
    public void onStart() {
        Timber.w("onStart");
        super.onStart();
        mBinding.map.onStart();
    }

    @Override
    public void onStop() {
        Timber.w("onStop");
        mBinding.map.onStop();
        mMarkerForStop.clear();
        mRouteShapes.clear();
        mRouteNames.clear();
        super.onStop();
    }

    @Override
    public void onPause() {
        Timber.w("onPause");
        mBinding.map.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        Timber.w("onResume");
        super.onResume();
        mBinding.map.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Timber.w("onSaveInstanceState");

        super.onSaveInstanceState(outState);
        try {
            //mBinding.map.onSaveInstanceState(outState);
        } catch (NullPointerException ignore) {
            // Is thrown in rare cases
            Timber.e(ignore);
        }
    }

    @Override
    public void onLowMemory() {
        Timber.w("onLowMemory");
        super.onLowMemory();
        mBinding.map.onLowMemory();
    }

    @Override
    public void onDestroy() {
        Timber.w("onDestroy");
        try {
            mBinding.map.onDestroy();
        } catch (NullPointerException ignore) {
            // Seems to happen sometimes
            Timber.e(ignore);
        }
        mRealm.close();
        super.onDestroy();
    }

    private void onMapReady(GoogleMap googleMap) {
        Timber.w("onMapReady");
        mMap = googleMap;
        mBinding.map.setTag(googleMap);

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
            mRouteNames.clear();

            StopDetailModel model = new StopDetailModel(mRealm, stop);

            Map<String, String> shapeIds = ScheduleUtils.getShapeIds(mRealm, model);

            int colorIndex = 0;

            for (Map.Entry<String, String> entry : shapeIds.entrySet()) {

                String routeName = entry.getKey();
                String shapeId = entry.getValue();

                final int routeColor = COLORS[colorIndex++ % COLORS.length];
                mApi.getShape(shapeId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(shapePaths -> {
                                    for (ShapePath shapePath : shapePaths) {
                                        List<LatLng> shape = PolyUtil.decode(shapePath.getPath());
                                        PolylineOptions line = new PolylineOptions()
                                                .addAll(shape)
                                                .width(10)
                                                .clickable(true)
                                                .geodesic(false)
                                                .visible(true)
                                                .zIndex(1)
                                                .color(routeColor);
                                        Polyline polyline = mMap.addPolyline(line);
                                        polyline.setVisible(true);
                                        mRouteShapes.add(polyline);
                                        mRouteNames.put(polyline, routeName);
                                    }
                                },
                                error -> {
                                    Timber.e(error);
                                });

            }

            mMap.setOnPolylineClickListener(polyline -> {
                String routeName = mRouteNames.get(polyline);
                Snackbar.make(((StopFragment.CoordinatorProvider) getActivity()).getCoordinator(), routeName, Snackbar.LENGTH_LONG).show();
            });

            mMap.setBuildingsEnabled(false);
            mMap.setIndoorEnabled(false);

            return false;
        });


        // Display all of the user's favourite stops
        mFavouriteStops = mRealm.where(FavouriteStops.class).equalTo("id", 0).findAllAsync();

        mFavouriteStops.addChangeListener(stops -> {
            Timber.w("Favourite stops changed.");

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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.map, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_show_location:

                RxPermissions.getInstance(getContext())
                        .request(Manifest.permission.ACCESS_FINE_LOCATION)
                        .subscribe(granted -> {
                            if (granted) {
                                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    mMap.setMyLocationEnabled(!mMap.isMyLocationEnabled());
                                }
                            }
                        });

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
