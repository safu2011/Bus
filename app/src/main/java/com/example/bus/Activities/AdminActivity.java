package com.example.bus.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.bus.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AdminActivity extends AppCompatActivity implements View.OnClickListener {
    private LinearLayout loadingScreen;
    final Boolean[] driverAvailable = {false,false};
    final int[] index = {0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        loadingScreen = findViewById(R.id.ll_loading_admin);
        findViewById(R.id.btn_drivers_info_admin).setOnClickListener(this);
        findViewById(R.id.btn_add_drivers_admin).setOnClickListener(this);
        findViewById(R.id.btn_track_driver_admin).setOnClickListener(this);
        findViewById(R.id.btn_logout_admin).setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_drivers_info_admin:
                Intent intent = new Intent(this,DriversInfoActivity.class);
                intent.putExtra("Parent node","Admins List");
                startActivity(intent);
                break;
            case R.id.btn_add_drivers_admin:
                startActivity(new Intent(this, AddDriverForAdminActivity.class));
                break;
            case R.id.btn_track_driver_admin:
                if(isNetworkAvailable())
                    checkForActiveDrivers();
                else
                    Toast.makeText(AdminActivity.this,"Check Internet Connection",Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_logout_admin:
                logout();
                break;
             }
    }


    private boolean isNetworkAvailable() {
        NetworkInfo networkInfo = ((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void checkForActiveDrivers(){
        showLoadingScreen();
        final ArrayList<String> driversList = new ArrayList<>();
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("Admins List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Drivers");
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    driverAvailable[1] = true;
                    driversList.add(ds.getKey());
                    DatabaseReference driversRef = FirebaseDatabase.getInstance().getReference("Producers List").child(ds.getKey());
                    driversRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            index[0]++;
                            if(dataSnapshot.child("IsActive").getValue(Boolean.class) == true){
                                showLoadingScreen();
                                driverAvailable[0] = true;
                                startActivity(new Intent(AdminActivity.this, ConsumerMaps.class));
                                hideLoadingScreen();
                                finish();
                            }else{
                                hideLoadingScreen();
                                Toast.makeText(AdminActivity.this,"No Active Drivers",Toast.LENGTH_SHORT).show();
                            }
                            Intent intent = new Intent("processing done");
                            sendBroadcast(intent);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(AdminActivity.this,"Opps Something went wrong !!!",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                if(driverAvailable[1] == false)
                    Toast.makeText(AdminActivity.this,"Add Driver",Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AdminActivity.this,"Opps Something went wrong !!!",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logout(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure ?")
                .setTitle("Sign Out")
                .setPositiveButton("Yes, Sign out.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        FirebaseAuth.getInstance().signOut();
                        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                                getString(R.string.user_type), Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(getString(R.string.user_type), "null");
                        editor.commit();
                        startActivity(new Intent(AdminActivity.this, LoginActivity.class));
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                }).create().show();
    }

    public void showLoadingScreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        loadingScreen.setVisibility(View.VISIBLE);
    }

    public void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }
}
