package com.example.bus.FragmentsAddNewDriver;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.bus.Activities.ConsumerActivity;
import com.example.bus.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static com.example.bus.Activities.AddNewDriverActivity.APPBAR_TITLE;
import static com.example.bus.Activities.AddNewDriverActivity.CURRENT_SELECTED_DRIVER;
import static com.example.bus.Activities.AddNewDriverActivity.INDEX;
import static com.example.bus.Activities.ProducerMapsActivity.PERMISSION_REQUEST_CODE_LOCATION;

public class ChoosePickUpLocationFragment extends Fragment implements OnMapReadyCallback, LocationListener {

    private static final String TAG = "mylog";
    private View root;
    private GoogleMap mMap;
    public LocationManager locationManager;
    private LatLng myLatlang;
    private Marker myMarker;
    private ProgressBar progressBar;
    private Handler mHandler;
    private LinearLayout lyHint;
    public static boolean IS_RUNNING_FIRST_TIME = true;
    private Button btnProceed;
    private LinearLayout loadingScreen;
    private AlertDialog dialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_choose_pick_up_location, container, false);
        lyHint = root.findViewById(R.id.ly_hint);
        btnProceed = root.findViewById(R.id.btn_send_req_add_new_driver);
        APPBAR_TITLE.setText("Select Pick Up Point");

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        progressBar = root.findViewById(R.id.pb_wating);

        getlocation();

        btnProceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getLocationDetails();
            }
        });


        return root;

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                if (IS_RUNNING_FIRST_TIME) {
                    lyHint.setVisibility(View.VISIBLE);
                    btnProceed.setVisibility(View.GONE);
                    HandlerHint();
                    IS_RUNNING_FIRST_TIME = false;
                } else {
                    changeMarkerPosition();
                }
            }
        });

    }

    @Override
    public void onLocationChanged(Location location) {
        myLatlang = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatlang, 14f));
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

    private Bitmap getMarkerBitmapFromView(int layoutId) {
        View customMarkerView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);

        //Todo  ImageView markerImageView = (ImageView) customMarkerView.findViewById(R.id.profile_image);
        // markerImageView.setImageResource(resId);

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

    private void getlocation() {
            if (ActivityCompat.checkSelfPermission(root.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(root.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE_LOCATION);
            } else {
                if (locationManager != null) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
                    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        myLatlang = new LatLng(location.getLatitude(), location.getLongitude());
                        if(mMap!=null)
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatlang, 14f));
                    } else {
                        Location loc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                        if(loc!=null){
                            myLatlang = new LatLng(loc.getLatitude(), loc.getLongitude());
                            if(mMap!=null)
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatlang, 14f));
                        }else{
                            Location loc1 = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            if(loc1 != null){
                                myLatlang = new LatLng(loc1.getLatitude(), loc1.getLongitude());
                                if(mMap!=null)
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatlang, 14f));
                            }
                        }
                    }
                } else {
                    locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                }
            }
    }

    private void getLocationDetails() {
        Geocoder geocoder;
        List<Address> addresses = null;
        geocoder = new Geocoder(getContext(), Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(myMarker.getPosition().latitude, myMarker.getPosition().longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        } catch (IOException e) {
            e.printStackTrace();
        }

        String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage(address)
                .setTitle("Pick up point")
                .setPositiveButton("Yes, Proceed this is my pick up point.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        sendRequestToDriver();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

        dialog = builder.create();
        dialog.show();
    }

    private void changeMarkerPosition() {
        mHandler = new Handler();
        progressBar.setMax(100);
        progressBar.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i <= 100; i++) {
                    final int currentProgressCount = i;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //Update the value background thread to UI thread
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(currentProgressCount);
                            if (currentProgressCount == 100) {
                                if (myMarker == null) {
                                    myMarker = mMap.addMarker(new MarkerOptions()
                                            .position(mMap.getCameraPosition().target)
                                            .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_self))));
                                } else {
                                    myMarker.setPosition(mMap.getCameraPosition().target);
                                }
                                btnProceed.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.INVISIBLE);
                            }
                        }
                    });
                }

            }
        }).start();
    }

    private void HandlerHint() {
        final Handler handlerHint = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                    handlerHint.post(new Runnable() {
                        @Override
                        public void run() {
                            lyHint.setVisibility(View.GONE);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void showLoadingScreen() {
        if (loadingScreen == null)
            loadingScreen = root.findViewById(R.id.ll_loading_pick_up_point);
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        loadingScreen.setVisibility(View.VISIBLE);
    }

    ;

    public void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void sendRequestToDriver() {
        showLoadingScreen();

        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("Consumers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        final DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference("Producers List").child(CURRENT_SELECTED_DRIVER.getId()).child("New Requests").child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                driverRef.child("Customer Name").setValue(dataSnapshot.child("Name").getValue(String.class));
                driverRef.child("Phone Number").setValue(dataSnapshot.child("Phone Number").getValue(String.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                hideLoadingScreen();
                Toast.makeText(root.getContext(), "Opps Something went wrong !!!", Toast.LENGTH_SHORT).show();
                dialog.cancel();
                return;
            }
        });

        driverRef.child("Pick up point latitude").setValue(myMarker.getPosition().latitude);
        driverRef.child("Pick up point longitude").setValue(myMarker.getPosition().longitude).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(Task<Void> task) {
                hideLoadingScreen();
                Toast.makeText(root.getContext(), "Request Sent", Toast.LENGTH_LONG).show();
                dialog.cancel();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                hideLoadingScreen();
                dialog.cancel();
                Toast.makeText(root.getContext(), "Opps Something went wrong !!!", Toast.LENGTH_SHORT).show();
                return;
            }
        });

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startActivity(new Intent(root.getContext(), ConsumerActivity.class));
        INDEX--;
        getActivity().finish();

    }


}
