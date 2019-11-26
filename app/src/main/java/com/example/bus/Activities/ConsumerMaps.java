package com.example.bus.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bus.ModelClasses.CustomerModelClass;
import com.example.bus.ModelClasses.DriverModelClass;
import com.example.bus.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;

import static com.example.bus.Activities.ProducerMapsActivity.PERMISSION_REQUEST_CODE_LOCATION;

public class ConsumerMaps extends AppCompatActivity implements OnMapReadyCallback , LocationListener {

    private GoogleMap mMap;
    private Marker customerMarker;
    private LatLng customerLatlang;
    private LocationManager locationManager;
    private ArrayList<Marker> driversMarkersList;
    private ArrayList<DriverModelClass> driversList;
    private LinearLayout loadingScreen;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter<ViewHolderRtCustomerMaps> adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_maps);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.customer_map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        getlocation();
        driversMarkersList = new ArrayList<>();
        driversList = new ArrayList<>();

        findViewById(R.id.my_customer_location_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(customerLatlang, 14f));
            }
        });

        findViewById(R.id.back_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ConsumerMaps.this,ConsumerActivity.class));
                finish();
            }
        });

        setRecyclerView();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        InfoWindowAdapter infoWindowAdapter = new InfoWindowAdapter();
        mMap.setInfoWindowAdapter(infoWindowAdapter);
        if(customerLatlang!=null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(customerLatlang, 14f));
            customerMarker = mMap.addMarker(new MarkerOptions()
                    .position(customerLatlang)
                    .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_self))));
        }

        getLocationDrivers();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(ConsumerMaps.this,ConsumerActivity.class));
        finish();
    }

    private void getlocation() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE_LOCATION);
            } else {
                if (locationManager != null) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
                    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        customerLatlang = new LatLng(location.getLatitude(), location.getLongitude());

                    } else {
                        Location loc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                        if(loc != null){
                            customerLatlang = new LatLng(loc.getLatitude(), loc.getLongitude());
                        }else{
                            Location loc1 = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            if(loc1 != null){
                                customerLatlang = new LatLng(loc1.getLatitude(), loc1.getLongitude());
                            }
                        }
                    }
                } else {
                    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
                    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        customerLatlang = new LatLng(location.getLatitude(), location.getLongitude());

                    } else {
                        Location loc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                        if(loc != null){
                            customerLatlang = new LatLng(loc.getLatitude(), loc.getLongitude());
                        }else{
                            Location loc1 = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            if(loc1 != null){
                                customerLatlang = new LatLng(loc1.getLatitude(), loc1.getLongitude());
                            }
                        }
                    }
                }
            }

        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if(mMap != null) {
            customerLatlang = new LatLng(location.getLatitude(), location.getLongitude());
            if (customerMarker == null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(customerLatlang, 14f));
                customerMarker = mMap.addMarker(new MarkerOptions()
                        .position(customerLatlang)
                        .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_self))));
            }
            customerMarker.setPosition(customerLatlang);
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    private Bitmap getMarkerBitmapFromView(int layoutId) {
        View customMarkerView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);
        if (layoutId == R.layout.marker_target_consumer) {
            //ImageView markerImageView = (ImageView) customMarkerView.findViewById(R.id.profile_image_target_consumer);
            // markerImageView.setImageResource(resId);
        } else if (layoutId == R.layout.marker_consumer) {
            //ImageView markerImageView = (ImageView) customMarkerView.findViewById(R.id.profile_image_consumer);
            // markerImageView.setImageResource(resId);
        } else {
            //  ImageView markerImageView = (ImageView) customMarkerView.findViewById(R.id.profile_image);
            // markerImageView.setImageResource(resId);
        }
        customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        customMarkerView.layout(0, 0, customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight());
        customMarkerView.buildDrawingCache();
        Bitmap returnedBitmap = Bitmap.createBitmap(customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_IN);
        Drawable drawable = customMarkerView.getBackground();
        if (drawable != null)
            drawable.draw(canvas);
        customMarkerView.draw(canvas);
        return returnedBitmap;

    }

    private void getLocationDrivers(){
        NetworkInfo networkInfo = ((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Consumers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Drivers");
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        showLoadingScreen();
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Producers List").child(ds.getKey());
                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.child("IsActive").getValue(Boolean.class) == true){
                                    String driverName = dataSnapshot.child("Name").getValue(String.class);
                                    String dutyat = dataSnapshot.child("Institute Name").getValue(String.class);
                                    double driverLatitude = dataSnapshot.child("Latitude").getValue(Double.class);
                                    double driverLongitude = dataSnapshot.child("Longitude").getValue(Double.class);
                                    driversMarkersList.add(getMarker(driverName,driverLatitude,driverLongitude));
                                    driversList.add(new DriverModelClass(ds.getKey(),driverName,"null",dutyat,"null",0,0));
                                    getArrivalTime(ds.getKey());
                                    adapter.notifyDataSetChanged();
                                }
                                hideLoadingScreen();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(ConsumerMaps.this, "Opps Something went wrong !!!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(ConsumerMaps.this, "Opps Something went wrong !!!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showLoadingScreen() {
        if (loadingScreen == null)
            loadingScreen = findViewById(R.id.ll_loading_customer_map);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        loadingScreen.setVisibility(View.VISIBLE);
    }

    private void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private Marker getMarker(String name, double lati, double longi){

        return mMap.addMarker(new MarkerOptions()
                .title(name)
                .position(new LatLng(lati, longi))
                .icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(),
                        R.drawable.bus_marker),
                        150,
                        150,
                        false))));
    }

    private void getArrivalTime(String driverId){
        DatabaseReference arrivalTimeRef = FirebaseDatabase.getInstance().getReference("Producers List").child(driverId);
        arrivalTimeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    double avgSpeed = 30;
                    if (dataSnapshot.child("Average speed").exists() && dataSnapshot.child("Average speed").getValue(double.class) > 0){
                        avgSpeed = dataSnapshot.child("Average speed").getValue(double.class);
                    }
                    double driverLati = dataSnapshot.child("Latitude").getValue(double.class);
                    double driverLongi = dataSnapshot.child("Longitude").getValue(double.class);

                    for(DriverModelClass driver : driversList){
                        if(driver.getId().equals(driverId))
                            driver.setArrivalTime(calculateArrivalTime(new LatLng(driverLati,driverLongi),customerLatlang,avgSpeed));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private String calculateArrivalTime(LatLng customerlatLng, LatLng myLatlang, double avgSpeed) {
        Location mylocation = new Location("");
        mylocation.setLatitude(myLatlang.latitude);
        mylocation.setLongitude(myLatlang.longitude);

        Location targetLoaction = new Location("");
        targetLoaction.setLatitude(customerlatLng.latitude);
        targetLoaction.setLongitude(customerlatLng.longitude);

        double distance = mylocation.distanceTo(targetLoaction);

        double distanceInKilometer = distance * 0.001;
        double estimatedTimeInHours = distanceInKilometer / avgSpeed;
        double estimatedTimeInMinitues = estimatedTimeInHours * 60;

        if (estimatedTimeInMinitues < 1) {
           return new DecimalFormat("##").format(estimatedTimeInMinitues * 60) + " sec";
        } else if (estimatedTimeInMinitues > 60) {
            return new DecimalFormat("##.#").format(estimatedTimeInMinitues / 60) + " hour";
        } else {
            return new DecimalFormat("##.#").format(estimatedTimeInMinitues) + " min";
        }
    }


//    public String calculateEstimatedTimeInMin(LatLng myLatLng, LatLng driver) {
//        Location mylocation = new Location("");
//        mylocation.setLatitude(myLatLng.latitude);
//        mylocation.setLongitude(myLatLng.longitude);
//
//        Location targetlocation = new Location("");
//        targetlocation.setLatitude(driver.latitude);
//        targetlocation.setLongitude(driver.longitude);
//
//        double distanceInMeters = mylocation.distanceTo(targetlocation); // distance in meters
//
//        int speedMetersPerMinute = 500; // 30kph average speed at start
//        double estematedDriveTimeInMinutes = distanceInMeters / speedMetersPerMinute;
//        if (estematedDriveTimeInMinutes < 1) {
//            return  new DecimalFormat("##.#").format(estematedDriveTimeInMinutes * 60) + " sec";
//        } else if (estematedDriveTimeInMinutes > 60) {
//            return  new DecimalFormat("##.#").format(estematedDriveTimeInMinutes / 60) + " hour";
//        } else {
//            return new DecimalFormat("##.#").format(estematedDriveTimeInMinutes) + " min";
//        }
//
//    }


    private void setRecyclerView(){
        recyclerView = findViewById(R.id.rv_customer_map);
        adapter = new RecyclerView.Adapter<ConsumerMaps.ViewHolderRtCustomerMaps>() {
            @NonNull
            @Override
            public ConsumerMaps.ViewHolderRtCustomerMaps onCreateViewHolder(@NonNull ViewGroup viewGroup, int ViewType) {
                return new ViewHolderRtCustomerMaps(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.marker_drivers_customer_maps_blueprint, viewGroup, false));
            }

            @Override
            public void onBindViewHolder(@NonNull ConsumerMaps.ViewHolderRtCustomerMaps viewHolderRt, final int i) {
                String driverName = driversMarkersList.get(i).getTitle();
                if(driverName.length()>5){

                    driverName  =  driverName.substring(0,8)+"...";

                    viewHolderRt.contact_name.setText(driverName);
                }else{

                    viewHolderRt.contact_name.setText(driverName); //Dont do any change

                }


                viewHolderRt.profileImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(driversMarkersList.get(i).getPosition(),22f));
                    }
                });
            }

            @Override
            public int getItemCount() {
                return driversMarkersList.size();
            }
        };
        recyclerView.setAdapter(adapter);
    }

    private class ViewHolderRtCustomerMaps extends RecyclerView.ViewHolder {
        TextView contact_name;
        ImageView profileImage;
        public ViewHolderRtCustomerMaps(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.civ_profile_image_marker_driver);
            contact_name = itemView.findViewById(R.id.tv_driver_name_marker_driver);

        }

    }

    public class InfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        private final View markerItemView;
        public InfoWindowAdapter() {
            markerItemView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.marker_info_view_blueprint, null);  // 1
        }
        @Override
        public View getInfoWindow(Marker marker) { // 2
            for(int i=0 ;i<driversList.size(); i++){
                if(marker.getTitle().equals(driversList.get(i).getName())){
                    getArrivalTime(driversList.get(i).getId());
                    TextView tvDriverName = markerItemView.findViewById(R.id.tv_marker_info_driver_name);
                    TextView tvDriverInstitute = markerItemView.findViewById(R.id.tv_marker_info_driver_duty_at);
                    TextView tvDriverArrivingTime = markerItemView.findViewById(R.id.tv_marker_info_driver_arriving_time);

                    tvDriverName.setText("Name : "+driversList.get(i).getName());
                    tvDriverInstitute.setText("Institute : "+driversList.get(i).getDutyAt());
                    tvDriverArrivingTime.setText("Arrival Time : "+driversList.get(i).getArrivalTime());
                }
            }
            return markerItemView;  // 4
        }
        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }
    }
}
