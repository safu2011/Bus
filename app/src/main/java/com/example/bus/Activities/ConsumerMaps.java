package com.example.bus.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bus.ModelClasses.DriverModelClass;
import com.example.bus.R;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;

import static com.example.bus.Activities.ProducerMapsActivity.PERMISSION_REQUEST_CODE_LOCATION;

public class ConsumerMaps extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker customerMarker;
    private LatLng customerLatlang;
    private ArrayList<Marker> driversMarkersList;
    private ArrayList<Marker> MyPickUpPointsList;
    private ArrayList<DriverModelClass> driversList;
    private LinearLayout loadingScreen;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter<ViewHolderRtCustomerMaps> adapter;
    private String rootRefNode;

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

        driversMarkersList = new ArrayList<>();
        MyPickUpPointsList = new ArrayList<>();

        driversList = new ArrayList<>();
        rootRefNode = getIntent().getStringExtra("Parent node");  // for reusability of activity of admin and parent

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
        InfoWindowAdapterForCustomer infoWindowAdapterForCustomer = new InfoWindowAdapterForCustomer();
        mMap.setInfoWindowAdapter(infoWindowAdapterForCustomer);

        getLocationDrivers();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(rootRefNode.equals("Consumers List"))
            startActivity(new Intent(ConsumerMaps.this,ConsumerActivity.class));
        else
            startActivity(new Intent(ConsumerMaps.this,AdminActivity.class));
        finish();
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
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference(rootRefNode).child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Drivers");
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
                                    double driverLatitude = dataSnapshot.child("Latitude").getValue(Double.class);
                                    double driverLongitude = dataSnapshot.child("Longitude").getValue(Double.class);

                                    double myPickUpPointLati = dataSnapshot.child("Customers").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Pick up point latitude").getValue(Double.class);
                                    double myPickUpPointLongi = dataSnapshot.child("Customers").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Pick up point longitude").getValue(Double.class);

                                    MyPickUpPointsList.add(mMap.addMarker(new MarkerOptions()
                                            .title("Me")
                                            .position(new LatLng(myPickUpPointLati,myPickUpPointLongi))
                                            .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_self)))));

                                    driversMarkersList.add(getMarker(driverName,driverLatitude,driverLongitude));

                                    ArrayList<String> institueNameList = new ArrayList<>();
                                    for(DataSnapshot ds1: dataSnapshot.child("Institute Name List").getChildren()){
                                        institueNameList.add(ds1.getKey());
                                    }


                                    DriverModelClass driver = new DriverModelClass(ds.getKey(),driverName,"null",institueNameList,"null",0,0);
                                    driver.setChildrenInVehicle(dataSnapshot.child("Children in vehicle").getValue(String.class));
                                    driversList.add(driver);
                                    getArrivalTime(ds.getKey());
                                    adapter.notifyDataSetChanged();
                                    setDriverLocationChangeListener(driver);
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

    private void setDriverLocationChangeListener(DriverModelClass driver){
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Producers List").child(driver.getId());
                ref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.child("IsActive").getValue(Boolean.class) == true){
                            double driverLatitude = dataSnapshot.child("Latitude").getValue(Double.class);
                            double driverLongitude = dataSnapshot.child("Longitude").getValue(Double.class);
                            for(Marker marker: driversMarkersList){
                                if(marker.getTitle().equals(dataSnapshot.child("Name"))){
                                    marker.setPosition(new LatLng(driverLatitude,driverLongitude));
                                }
                            }
                        }else{
                            String driverName = dataSnapshot.child("Name").getValue(String.class);
                            for(Marker marker: driversMarkersList){
                                if(marker.getTitle().equals(driverName)){
                                    marker.remove();
                                    driversMarkersList.remove(marker);
                                    for(DriverModelClass driver : driversList){
                                        if(driver.getName().equals(driverName)){
                                            driversList.remove(driver);
                                        }
                                    }

                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }
                        hideLoadingScreen();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(ConsumerMaps.this, "Opps Something went wrong !!!", Toast.LENGTH_SHORT).show();
                    }
                });
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
        if(myLatlang!=null){
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
        }else{
            return "Estimating";
        }
    }

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

    public class InfoWindowAdapterForCustomer implements GoogleMap.InfoWindowAdapter {
        private final View markerItemView;
        public InfoWindowAdapterForCustomer() {
            markerItemView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.marker_info_view_blueprint, null);  // 1
        }

        @Override
        public View getInfoWindow(Marker marker) { // 2
            if(!marker.getTitle().equals("Me")){
                for(int i=0 ;i<driversList.size(); i++){
                    if(driversList.get(i) != null) {
                        if (marker.getTitle().equals(driversList.get(i).getName())) {
                            TextView tvDriverName = markerItemView.findViewById(R.id.tv_marker_info_driver_name);
                            TextView tvDriverInstitute = markerItemView.findViewById(R.id.tv_marker_info_driver_duty_at);
                            TextView tvDriverArrivingTime = markerItemView.findViewById(R.id.tv_marker_info_driver_arriving_time);

                            tvDriverName.setText(driversList.get(i).getName());
                            if(driversList.get(i).getDutyAt().size()>1){
                                String institueNames = driversList.get(i).getDutyAt().get(0);
                                for(int z=1 ; z<driversList.get(i).getDutyAt().size(); z++){
                                    institueNames = institueNames +"\n"+driversList.get(i).getDutyAt().get(z);
                                }
                                tvDriverInstitute.setText(institueNames);
                            }else{
                                tvDriverInstitute.setText("Institute : " + driversList.get(i).getDutyAt());
                            }


                            if (rootRefNode.equals("Consumers List")) {
                                getArrivalTime(driversList.get(i).getId());
                                tvDriverArrivingTime.setText("Arrival Time : " + driversList.get(i).getArrivalTime());
                            } else
                                tvDriverArrivingTime.setText("Children in vehicle : " + driversList.get(i).getChildrenInVehicle());
                        }
                        return markerItemView;
                    }
                 }
            }
            return null;
        }
        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }
    }


}
