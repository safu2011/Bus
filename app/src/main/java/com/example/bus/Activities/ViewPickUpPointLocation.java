package com.example.bus.Activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bus.ModelClasses.CustomerModelClass;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ViewPickUpPointLocation extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Button btnProceed;
    private LinearLayout loadingScreen;
    private CustomerModelClass currentSelectedCustomer;
    private TextView tvMiddleCircle;
    private int seatsOccupied;
    Marker marker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_choose_pick_up_location);
        tvMiddleCircle = findViewById(R.id.tv_middle_circle);
        tvMiddleCircle.setVisibility(View.GONE);
        currentSelectedCustomer = (CustomerModelClass) getIntent().getSerializableExtra("selectedCustomer");
        seatsOccupied = getIntent().getIntExtra("seatsOccupied",0);
        btnProceed = findViewById(R.id.btn_send_req_add_new_driver);
        btnProceed.setVisibility(View.VISIBLE);
        btnProceed.setText("Accept Request");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }


        btnProceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                acceptRequest();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentSelectedCustomer.getCustomerLatitude(), currentSelectedCustomer.getCustomerLongitude()), 14f));

        marker = mMap.addMarker(new MarkerOptions()
                .title(getLocationDetails())
                .position(new LatLng(currentSelectedCustomer.getCustomerLatitude(), currentSelectedCustomer.getCustomerLongitude()))
                .icon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView(R.layout.marker_consumer))));


    }

    private Bitmap getMarkerBitmapFromView(int layoutId) {
        View customMarkerView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layoutId, null);

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


    public void showLoadingScreen() {
        if (loadingScreen == null)
            loadingScreen = this.findViewById(R.id.ll_loading_pick_up_point);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        loadingScreen.setVisibility(View.VISIBLE);
    }

    ;

    public void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void acceptRequest() {
        showLoadingScreen();

        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Customers").child(currentSelectedCustomer.getId());

        myRef.child("Name").setValue(currentSelectedCustomer.getCustomerName());
        myRef.child("Phone Number").setValue(currentSelectedCustomer.getCustomerPhoneNumber());
        myRef.child("Pick up point latitude").setValue(currentSelectedCustomer.getCustomerLatitude());
        myRef.child("Pick up point longitude").setValue(currentSelectedCustomer.getCustomerLongitude()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(Task<Void> task) {
                hideLoadingScreen();
                FirebaseDatabase.getInstance().getReference("Consumers List").child(currentSelectedCustomer.getId()).child("Drivers").child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue("Service Active");
                FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child("New Requests")
                        .child(currentSelectedCustomer.getId())
                        .removeValue();

                seatsOccupied++;
                DatabaseReference capacityRef = FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Seats Occupied");
                capacityRef.setValue(seatsOccupied);

                Toast.makeText(ViewPickUpPointLocation.this, "Request Accepted", Toast.LENGTH_LONG).show();

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                startActivity(new Intent(ViewPickUpPointLocation.this, ProducerMapsActivity.class));
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                hideLoadingScreen();
                Toast.makeText(ViewPickUpPointLocation.this, "Opps Something went wrong !!!", Toast.LENGTH_SHORT).show();
                return;
            }
        });


    }

    private String getLocationDetails() {
        Geocoder geocoder;
        List<Address> addresses = null;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(currentSelectedCustomer.getCustomerLatitude(), currentSelectedCustomer.getCustomerLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        } catch (IOException e) {
            e.printStackTrace();
        }

        return addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
    }

}
