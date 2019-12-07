package com.example.bus.Activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;

import com.applandeo.materialcalendarview.EventDay;
import com.example.bus.directionhelpers.FetchURL;
import com.example.bus.directionhelpers.TaskLoadedCallback;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;

import androidx.core.app.ActivityCompat;

import android.os.Bundle;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.bus.ModelClasses.CustomerModelClass;
import com.example.bus.Services.ProducerService;
import com.example.bus.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


import de.hdodenhof.circleimageview.CircleImageView;

import static com.example.bus.InfoFragmentUi.ConsumerRequestFragment.NEW_CUSTOMER_ADDED;
import static com.example.bus.Services.ProducerService.currentTargetedCustomer;
import static com.example.bus.Services.ProducerService.customersList;


public class ProducerMapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, NavigationView.OnNavigationItemSelectedListener, TaskLoadedCallback {

    public static final int PERMISSION_REQUEST_CODE_LOCATION = 1;
    private ConnectivityManager CONNECTIVITY_MANAGER;
    private GoogleMap mMap;
    public LocationManager locationManager;
    private LatLng myLatlang;
    private Marker myMarker;
    private ArrayList<Marker> customersMarkerList;
    private BroadcastReceiver broadcastReceiver;
    private TextView tvDistanceTargetCustomer, myAvgSpeedTvTargetCustomer, estimatedTargetCustomerTime, tvTargetCustomerName, tvTargetCustomerNumber, tvNotfication;
    private BottomSheetBehavior mBottomSheetBehaviour;
    private View nestedScrollView;
    private Button btnMyLocation, btnStartTtip, btnStopTrip, btnStartNavigation;
    private LinearLayout lyNoInternet;
    private String currentDate;


    private LinearLayout loadingScreen;
    private ConnectivityManager connectivityManager;
    private Polyline currentPolyline;
    private DatabaseReference myRef;

    @Override
    protected void onResume() {
        super.onResume();
        if (!isNetworkAvailable()) {
            lyNoInternet = findViewById(R.id.ly_no_internet);
            lyNoInternet.setVisibility(View.VISIBLE);
        }
        if (currentTargetedCustomer != null) {
            updateMarkerIcon();
            setLeaveListeners();
            btnStartTtip.setVisibility(View.GONE);
        }else if(NEW_CUSTOMER_ADDED){
            customersList = null;
            getCustomersInfo();
            NEW_CUSTOMER_ADDED = false;
        } else {
            if (isNetworkAvailable())
                FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("IsActive").setValue(false);
        }
        IntentFilter intentFilters = new IntentFilter();
        intentFilters.addAction("update marker");
        intentFilters.addAction("distance value");
        intentFilters.addAction("update avg values");
        intentFilters.addAction("update estimated time value");
        intentFilters.addAction("update polyline");
        intentFilters.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        setNewRequestsListeners();


        registerReceiver(broadcastReceiver, intentFilters);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        setContentView(R.layout.activity_maps);
        CONNECTIVITY_MANAGER = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        init();

        broadcastReceiver = new MyBroadcastReceiver();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setCompassEnabled(false);
        if (myLatlang != null) {
            myMarker = mMap.addMarker(new MarkerOptions()
                    .position(myLatlang)
                    .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_self))));
            updateLocationInDatabase();
        } else if (getLocationFromDatabase() != null) {
            myLatlang = getLocationFromDatabase();
            myMarker = mMap.addMarker(new MarkerOptions()
                    .position(myLatlang)
                    .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_self))));
            updateLocationInDatabase();
        }

        if (currentTargetedCustomer == null) {
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatlang, 14f));
        } else {
            mMap.getUiSettings().setZoomControlsEnabled(false);
            currentPolyline = mMap.addPolyline(currentTargetedCustomer.getCustomerPolyline());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(myLatlang.latitude - 0.00008, myLatlang.longitude), 22f));
        }

        // drawing markers on Map if not drawn before
        if (customersList != null && customersList.size() > 0) {
            for (CustomerModelClass customer : customersList) {
                if ((currentTargetedCustomer != null) && (customer.getCustomerName().equals(currentTargetedCustomer.getCustomerName()))) {
                    customersMarkerList.add(mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(customer.getCustomerLatitude(), customer.getCustomerLongitude()))
                            .title(customer.getCustomerName())
                            .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_target_consumer)))));
                } else {
                    customersMarkerList.add(mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(customer.getCustomerLatitude(), customer.getCustomerLongitude()))
                            .title(customer.getCustomerName())
                            .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_consumer)))));
                }
            }
        }


    }

    @Override
    public void onLocationChanged(Location location) {
        myLatlang = new LatLng(location.getLatitude(), location.getLongitude());
        if (myMarker != null && mMap != null) {
            myMarker.setPosition(myLatlang);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatlang, 14f));
        } else if (myMarker == null) {
            myMarker = mMap.addMarker(new MarkerOptions()
                    .position(myLatlang)
                    .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_self))));
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {

            case PERMISSION_REQUEST_CODE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getlocation();
                } else {

                    Toast.makeText(getApplicationContext(), "Permission Denied, You cannot access location data.", Toast.LENGTH_LONG).show();

                }
                break;

        }
    }

    @Override
    public void onBackPressed() {
        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (mBottomSheetBehaviour.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
            mBottomSheetBehaviour.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_consumers) {
            Intent intent = new Intent(ProducerMapsActivity.this, InfoActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_start_trip) {
            startTrip();
        } else if (id == R.id.nav_stopService) {
            stopTrip();
        } else if (id == R.id.nav_sign_out) {
            if (currentTargetedCustomer != null) {
                if (isNetworkAvailable()) {
                    FirebaseDatabase.getInstance().getReference("Consumers List").child(currentTargetedCustomer.getId()).child("Arrived").setValue("True");
                    //ask to quit trip first
                }
            } else {

                if (isNetworkAvailable()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Are you sure ?")
                            .setTitle("Sign Out")
                            .setPositiveButton("Yes, Sign out.", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (isNetworkAvailable()) {
                                        FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("IsActive").setValue(false);
                                        Intent intent = new Intent(ProducerMapsActivity.this, ProducerService.class);
                                        stopService(intent);
                                        currentTargetedCustomer = null;
                                        FirebaseAuth.getInstance().signOut();
                                        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                                                getString(R.string.user_type), Context.MODE_PRIVATE);
                                        SharedPreferences.Editor editor = sharedPref.edit();
                                        editor.putString(getString(R.string.user_type), "null");
                                        editor.commit();
                                        Toast.makeText(ProducerMapsActivity.this, "Logging Out...", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(ProducerMapsActivity.this, LoginActivity.class));
                                        finish();
                                    }
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            }).create().show();
                }

            }
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void init() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        getlocation();
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        nestedScrollView = findViewById(R.id.nestedScrollView);
        mBottomSheetBehaviour = BottomSheetBehavior.from(nestedScrollView);

        tvTargetCustomerName = findViewById(R.id.tv_target_customer_name_bottom_sheet);
        tvTargetCustomerNumber = findViewById(R.id.tv_target_customer_number_bottom_sheet);
        tvDistanceTargetCustomer = findViewById(R.id.tv_distance_bottom_sheet);
        myAvgSpeedTvTargetCustomer = findViewById(R.id.tv_avg_speed_bottom_sheet);
        estimatedTargetCustomerTime = findViewById(R.id.tv_estimated_time_bottom_sheet);
        tvNotfication = findViewById(R.id.tv_notification);
        loadingScreen = findViewById(R.id.ll_loading);
        btnMyLocation = findViewById(R.id.my_location_btn);
        btnStartTtip = findViewById(R.id.start_trip_btn);
        btnStopTrip = findViewById(R.id.stop_trip_btn);
        btnStartNavigation = findViewById(R.id.start_navigation_btn);
        lyNoInternet = findViewById(R.id.ly_no_internet);

        currentDate = getCurrentDate();
        //if app was not running already
        getCustomersInfo();
        if (currentTargetedCustomer != null) {
            setCustomerImageBottomSheet(R.drawable.user_img);
            nestedScrollView.setVisibility(View.VISIBLE);
            mBottomSheetBehaviour.setState(BottomSheetBehavior.STATE_EXPANDED);
            btnStartTtip.setVisibility(View.GONE);
        }
        customersMarkerList = new ArrayList<>();

        NavigationView navigationView = findViewById(R.id.nav_view);
        Button btn = findViewById(R.id.open_btn);

        View headerView = navigationView.getHeaderView(0);
        CircleImageView imageView = headerView.findViewById(R.id.nav_header_producer_pic);
        Glide
                .with(this)
                .load(R.drawable.pic)
                .into(imageView);


        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawer.openDrawer(GravityCompat.START);
            }
        });


        btnMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatlang, 16f));
            }
        });

        btnStartTtip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTrip();
            }
        });

        btnStopTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTrip();
            }
        });

        btnStartNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + currentTargetedCustomer.getCustomerLatitude() + "," + currentTargetedCustomer.getCustomerLongitude() + "&mode=d"));
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });
        navigationView.setNavigationItemSelectedListener(this);

    }

    private void getCustomersInfo() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (customersList == null && networkInfo != null && networkInfo.isConnected()) {
            customersList = new ArrayList<>();
            if (networkInfo != null && networkInfo.isConnected()) {
                myRef = FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                showLoadingScreen();
                setNewRequestsListeners();
                myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(currentTargetedCustomer==null) {
                            for (DataSnapshot ds : dataSnapshot.child("Customers").getChildren()) {
                                String customerId = ds.getKey();
                                String customerName = ds.child("Name").getValue(String.class);
                                String customerPhoneNumber = ds.child("Phone Number").getValue(String.class);
                                String latti = ds.child("Pick up point latitude").getValue(Double.class).toString();
                                String longi = ds.child("Pick up point longitude").getValue(Double.class).toString();
                                int notificationDistance;
                                if (ds.child("Notification distance values").exists()) {
                                    notificationDistance = ds.child("Notification distance values").getValue(int.class);
                                } else {
                                    notificationDistance = 50;
                                }
                                //these two lines
                                CustomerModelClass currentCustomer = new CustomerModelClass(customerId, customerName, customerPhoneNumber, latti, longi, ds.getRef() + "", false, notificationDistance);
                                customersList.add(currentCustomer);
                            }

                            // drawing markers on Map if not drawn before
                            if (customersMarkerList.size() == 0) {
                                for (CustomerModelClass customer : customersList) {
                                    if ((currentTargetedCustomer != null) && (customer.getCustomerName().equals(currentTargetedCustomer.getCustomerName()))) {
                                        customersMarkerList.add(mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(customer.getCustomerLatitude(), customer.getCustomerLongitude()))
                                                .title(customer.getCustomerName())
                                                .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_target_consumer)))));
                                    } else {
                                        customersMarkerList.add(mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(customer.getCustomerLatitude(), customer.getCustomerLongitude()))
                                                .title(customer.getCustomerName())
                                                .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_consumer)))));
                                    }
                                }
                            } else {
                                for (Marker marker : customersMarkerList) {
                                    marker.remove();
                                }
                                customersMarkerList.clear();
                                for (CustomerModelClass customer : customersList) {
                                    if ((currentTargetedCustomer != null) && (customer.getCustomerName().equals(currentTargetedCustomer.getCustomerName()))) {
                                        customersMarkerList.add(mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(customer.getCustomerLatitude(), customer.getCustomerLongitude()))
                                                .title(customer.getCustomerName())
                                                .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_target_consumer)))));
                                    } else {
                                        customersMarkerList.add(mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(customer.getCustomerLatitude(), customer.getCustomerLongitude()))
                                                .title(customer.getCustomerName())
                                                .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_consumer)))));
                                    }
                                }
                            }
                            setLeaveListeners();
                            hideLoadingScreen();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        hideLoadingScreen();
                        Toast.makeText(ProducerMapsActivity.this, "Can't Fetch Customers List !!!", Toast.LENGTH_SHORT).show();
                    }
                });

            } else {
                lyNoInternet.setVisibility(View.VISIBLE);
                Toast.makeText(ProducerMapsActivity.this, "you are offline !", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setNewRequestsListeners() {
        DatabaseReference myRefGetCustomersRequests = FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("New Requests");
        final int[] noOfCustomersReq = {0};
        myRefGetCustomersRequests.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean requestsAvailable = false;
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    requestsAvailable = true;
                }
                if (!requestsAvailable) {
                    tvNotfication.setVisibility(View.GONE);
                } else {
                    tvNotfication.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }

    private void setLeaveListeners(){
        DatabaseReference leaveRef = FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Leave Dates");
        leaveRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for(int i =0 ;i<customersMarkerList.size() ;i++){
                    customersMarkerList.get(i).setIcon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_consumer)));
                    customersList.get(i).setIsOnLeave(false);
                    customersMarkerList.get(i).setTitle(customersList.get(i).getCustomerName());
                }

                if (dataSnapshot.exists()) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        String customerId = ds.getKey();
                        ArrayList<String> daysOffList = new ArrayList<>();
                        for (DataSnapshot dayOffTimeSnap : dataSnapshot.child(customerId).getChildren()) {
                            String date = dayOffTimeSnap.getKey();
                            daysOffList.add(date);
                        }

                        for (int i = 0; i < customersList.size(); i++) {
                            if (customersList.get(i).getId().equals(customerId)) {
                                customersList.get(i).setLeaveDates(daysOffList);
                                if (checkIfOnLeave(daysOffList)) {
                                    customersList.get(i).setIsOnLeave(true);
                                    for (Marker customerMarker : customersMarkerList) {
                                        if (customerMarker.getTitle().equals(customersList.get(i).getCustomerName())) {
                                            customerMarker.setTitle(customersList.get(i).getCustomerName()+" (On Leave)");
                                            customerMarker.setIcon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_consumer_leave)));
                                        }
                                    }

                                }
                            }

                        }

                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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
                        myLatlang = new LatLng(location.getLatitude(), location.getLongitude());

                    } else {
                        Location loc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                        if (loc != null) {
                            myLatlang = new LatLng(loc.getLatitude(), loc.getLongitude());
                        } else {
                            Location loc1 = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            if (loc1 != null) {
                                myLatlang = new LatLng(loc1.getLatitude(), loc1.getLongitude());
                            }
                        }
                    }
                } else {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
                    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        myLatlang = new LatLng(location.getLatitude(), location.getLongitude());

                    } else {
                        Location loc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                        if (loc != null) {
                            myLatlang = new LatLng(loc.getLatitude(), loc.getLongitude());
                        } else {
                            Location loc1 = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            if (loc1 != null) {
                                myLatlang = new LatLng(loc1.getLatitude(), loc1.getLongitude());
                            }
                        }
                    }
                }
            }

        }
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

    public void updateMarkerIcon() {
        if (mMap != null && myLatlang != null) {
            if (btnStopTrip.getVisibility() == View.VISIBLE) {  // to check if the user is stoping a trip
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatlang, 14f));
            } else {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatlang, 22f));
            }

            for (Marker customerMarker : customersMarkerList) {
                if (currentTargetedCustomer != null && customerMarker.getTitle().equals(currentTargetedCustomer.getCustomerName())) {
                    customerMarker.setIcon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_target_consumer)));
                    setCustomerImageBottomSheet(R.drawable.user_img);
                    tvTargetCustomerName.setText("Name : " + currentTargetedCustomer.getCustomerName());
                    tvTargetCustomerNumber.setText("Number : " + currentTargetedCustomer.getCustomerPhoneNumber());
                } else {
                    if (customerMarker.getTitle().contains(" (On Leave)")) {
                        customerMarker.setIcon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_consumer_leave)));
                    } else {
                        customerMarker.setIcon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_consumer)));
                    }

                }
            }
            if (currentTargetedCustomer == null) {
                btnStopTrip.setVisibility(View.INVISIBLE);
                btnStartTtip.setVisibility(View.VISIBLE);
                nestedScrollView.setVisibility(View.GONE);
                mBottomSheetBehaviour.setState(BottomSheetBehavior.STATE_COLLAPSED);
                mMap.getUiSettings().setZoomControlsEnabled(true);
            }
        }
    }

    public void showLoadingScreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        loadingScreen.setVisibility(View.VISIBLE);
    }

    public void hideLoadingScreen() {
        if(loadingScreen.getVisibility()==View.VISIBLE){
            loadingScreen.setVisibility(View.GONE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }

    private void setCustomerImageBottomSheet(int id) {
        CircleImageView imageViewTarget = findViewById(R.id.target_profile_image_bottom_sheet);
        Glide
                .with(this)
                .load(id)
                .into(imageViewTarget);
    }

    private void startTrip() {
        if (isNetworkAvailable()) {
            if (customersList != null && customersList.size() > 0) {
                Intent intent = new Intent(ProducerMapsActivity.this, ProducerService.class);
                intent.putExtra("myLatlang", myLatlang);
                startService(intent);
                mMap.getUiSettings().setZoomControlsEnabled(false);
                btnStopTrip.setVisibility(View.VISIBLE);
                btnStartTtip.setVisibility(View.GONE);
                nestedScrollView.setVisibility(View.VISIBLE);
                mBottomSheetBehaviour.setState(BottomSheetBehavior.STATE_EXPANDED);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("IsActive").setValue(true);
                }
            } else {
                Toast.makeText(this, "Add Customer First !!!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please check your Internet", Toast.LENGTH_LONG).show();
        }
    }

    private void stopTrip() {
        if (currentTargetedCustomer != null) {
            if (currentPolyline != null)
                currentPolyline.remove();
            if (isNetworkAvailable()) {
                for (CustomerModelClass customer : customersList) {
                    FirebaseDatabase.getInstance().getReference("Consumers List").child(customer.getId()).child("Arrived").removeValue();
                }
            }
            Intent intent = new Intent(ProducerMapsActivity.this, ProducerService.class);
            stopService(intent);
            currentTargetedCustomer = null;
            updateMarkerIcon();

        } else {
            Toast.makeText(this, "Start Trip First !", Toast.LENGTH_LONG).show();
        }
    }

    private void updateLocationInDatabase() {
        if (myLatlang != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Latitude").setValue(myLatlang.latitude);
                FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Longitude").setValue(myLatlang.longitude);
            }
        }
    }

    private LatLng getLocationFromDatabase() {
        final double[] lati = new double[1];
        final double[] longi = new double[1];

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            DatabaseReference refLati = FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Latitude");
            refLati.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    lati[0] = dataSnapshot.getValue(double.class);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
            DatabaseReference refLongi = FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Longitude");
            refLongi.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    longi[0] = dataSnapshot.getValue(double.class);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            return new LatLng(lati[0], longi[0]);
        }

        return null;

    }

    private boolean isNetworkAvailable() {
        NetworkInfo activeNetworkInfo = CONNECTIVITY_MANAGER.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private Boolean checkIfOnLeave(ArrayList<String> datesList) {

        for (String date : datesList) {
            if (date.equals(currentDate)) {
                return true;
            }
        }
        return false;
    }

    private String getCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);

        return calendar.getTimeInMillis() + "";
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() != null) {
                NetworkInfo ni = (NetworkInfo) intent.getExtras().get(ConnectivityManager.EXTRA_NETWORK_INFO);
                if (ni != null && ni.getState() == NetworkInfo.State.CONNECTED) {
                    lyNoInternet.setVisibility(View.GONE);
                    getCustomersInfo();
                } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                    lyNoInternet.setVisibility(View.VISIBLE);
                }
            }
            if (tvDistanceTargetCustomer != null) {
                if (intent.getAction().equals("distance value")) {
                    tvDistanceTargetCustomer.setText(intent.getStringExtra("distance to target"));
                } else if (intent.getAction().equals("update marker")) {
                    updateMarkerIcon();
                } else if (intent.getAction().equals("update avg values")) {
                    estimatedTargetCustomerTime.setText(intent.getStringExtra("estimated time"));
                    myAvgSpeedTvTargetCustomer.setText(intent.getStringExtra("average speed"));
                } else if (intent.getAction().equals("update estimated time value")) {
                    estimatedTargetCustomerTime.setText(intent.getStringExtra("estimated time"));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(myLatlang.latitude - 0.00008, myLatlang.longitude), 22f));
                } else if (intent.getAction().equals("update polyline")) {
                    if (currentTargetedCustomer != null)
                        new FetchURL(context).execute(getUrl(myLatlang, new LatLng(currentTargetedCustomer.getCustomerLatitude(), currentTargetedCustomer.getCustomerLongitude()), "driving"), "driving");
                    else
                        currentPolyline.remove();
                }
            }
        }
    }

    // CODE FOR NAVIGATION
    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = mMap.addPolyline((PolylineOptions) values[0]);
        currentTargetedCustomer.setCustomerPolylineOptions((PolylineOptions) values[0]);
    }

    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getResources().getString(R.string.directions_api);
        return url;
    }

}
