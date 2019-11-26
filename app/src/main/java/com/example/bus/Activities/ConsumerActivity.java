package com.example.bus.Activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.bus.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class ConsumerActivity extends AppCompatActivity implements View.OnClickListener {
    private BroadcastReceiver broadcastReceiver;
    final Boolean[] driverAvailable = {false,false};
    final int[] index = {0};
    private LinearLayout loadingScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);
        loadingScreen = findViewById(R.id.ll_loading_consumer);
        findViewById(R.id.btn_drivers_info).setOnClickListener(this);
        findViewById(R.id.btn_add_drivers).setOnClickListener(this);
        findViewById(R.id.btn_track_driver).setOnClickListener(this);
        findViewById(R.id.btn_leave).setOnClickListener(this);
        findViewById(R.id.btn_logout).setOnClickListener(this);
        broadcastReceiver = new MyCustomerBroadcastReceiver();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_customer_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.nav_sign_out_customer_activity:
                logout();
                break;
            case R.id.nav_settings_customer_activity:
                startActivity(new Intent(this,ConsumerSettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_drivers_info:
                startActivity(new Intent(this, DriversInfoActivity.class));
                break;
            case R.id.btn_add_drivers:
                startActivity(new Intent(this, AddNewDriverActivity.class));
                break;
            case R.id.btn_track_driver:
                if(isNetworkAvailable())
                    checkForActiveDrivers();
                else
                    Toast.makeText(ConsumerActivity.this,"Check Internet Connection",Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_leave:
                startActivity(new Intent(this, ConsumerLeaveActivity.class));
                break;
            case R.id.btn_logout:
                logout();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilters = new IntentFilter();
        intentFilters.addAction("processing done");
        registerReceiver(broadcastReceiver, intentFilters);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    private void logout(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure ?")
                .setTitle("Sign Out")
                .setPositiveButton("Yes, Sign out.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        FirebaseDatabase.getInstance().getReference("Consumers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Messaging Token").setValue("null")
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    FirebaseDatabase.getInstance().getReference("Consumers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Arrived").removeValue()
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {

                                                    if(task.isSuccessful()){
                                                        FirebaseAuth.getInstance().signOut();
                                                        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                                                                getString(R.string.user_type), Context.MODE_PRIVATE);
                                                        SharedPreferences.Editor editor = sharedPref.edit();
                                                        editor.putString(getString(R.string.user_type), "null");
                                                        editor.commit();
                                                        startActivity(new Intent(ConsumerActivity.this, LoginActivity.class));
                                                        finish();
                                                    }else{
                                                        Log.d("ConsumerActivity", "onComplete: "+task.getException().getMessage());
                                                        Toast.makeText(ConsumerActivity.this,"Opps something went wrong !!!!",Toast.LENGTH_LONG).show();
                                                    }
                                                }
                                            });
                                } else{
                                    Log.d("ConsumerActivity", "onComplete: "+task.getException().getMessage());
                                    Toast.makeText(ConsumerActivity.this,"Opps something went wrong !!!!",Toast.LENGTH_LONG).show();
                                }


                            }
                        });


                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                }).create().show();
    }

    private boolean isNetworkAvailable() {
        NetworkInfo networkInfo = ((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void checkForActiveDrivers(){
        showLoadingScreen();
        final ArrayList<String> driversList = new ArrayList<>();
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("Consumers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Drivers");
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
                                startActivity(new Intent(ConsumerActivity.this, ConsumerMaps.class));
                                hideLoadingScreen();
                                finish();
                            }
                            Intent intent = new Intent("processing done");
                            sendBroadcast(intent);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(ConsumerActivity.this,"Opps Something went wrong !!!",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                if(driverAvailable[1] == false)
                    Toast.makeText(ConsumerActivity.this,"Add Driver",Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ConsumerActivity.this,"Opps Something went wrong !!!",Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void showLoadingScreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        loadingScreen.setVisibility(View.VISIBLE);
    }

    public void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    public class MyCustomerBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("processing done") && driverAvailable[0] == false && index[0]==driverAvailable.length) {
                    index[0] = 0;
                    hideLoadingScreen();
                    Toast.makeText(ConsumerActivity.this,"All drivers are offline",Toast.LENGTH_SHORT).show();
            }
        }
    }


}
