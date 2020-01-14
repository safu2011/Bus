package com.example.bus.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.bus.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class UserTypeActivity extends AppCompatActivity implements View.OnClickListener {

    private LinearLayout lyUtParent, lyUtDriver, lyUtAdmin;
    private LocationManager locationManager;
    private final String[] PERMISSIONS = {
            Manifest.permission.SEND_SMS,
            android.Manifest.permission.ACCESS_FINE_LOCATION};
    private static final int RC_PERMISSION_ALL = 1212;
    private static final int RC_LOCAION_ON = 1211;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_tyoe);

        lyUtParent = findViewById(R.id.ly_user_type_parent);
        lyUtDriver = findViewById(R.id.ly_user_type_driver);
        lyUtAdmin = findViewById(R.id.ly_user_type_admin);

        lyUtParent.setOnClickListener(this);
        lyUtDriver.setOnClickListener(this);
        lyUtAdmin.setOnClickListener(this);


    }

    private void changeActivity(String type){
        Intent intent = new Intent(UserTypeActivity.this,LoginActivity.class);
        intent.putExtra("user",type);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.ly_user_type_parent:
                changeActivity("parent");
                return;
            case R.id.ly_user_type_driver:
                changeActivity("driver");
                return;
            case R.id.ly_user_type_admin:
                changeActivity("admin");
                return;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!hasPermissions(this, PERMISSIONS)) {
            requestAllPermissions();
        } else if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showAlerDialogForGPS();
        }else{
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                        getString(R.string.user_type), Context.MODE_PRIVATE);
                String userType = sharedPref.getString(getString(R.string.user_type), "null");
                if (userType.equals("Producer")) {
                    Intent intent = new Intent(this, ProducerMapsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("online", true);
                    startActivity(intent);
                } else if (userType.equals("Consumer")) {
                    Intent intent = new Intent(this, ConsumerActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("online", true);
                    if(isNetworkAvailable()){
                        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                            @Override
                            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                FirebaseDatabase.getInstance().getReference("Consumers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Messaging Token").setValue( task.getResult().getToken() );
                            }
                        });
                        startActivity(intent);
                    }else{
                        Toast.makeText(this,"No Internet !!!",Toast.LENGTH_LONG).show();
                    }

                } else if (userType.equals("Admin")){
                    Intent intent = new Intent(this, AdminActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            }
        }


    }

    private boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void requestAllPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Allow Permissions and turn on location")
                    .setCancelable(false)
                    .setMessage("In order for this app to function properly, storage and location permissions must be granted")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                ActivityCompat.requestPermissions(UserTypeActivity.this, PERMISSIONS,
                                        RC_PERMISSION_ALL);
                            }
                        }
                    })
                    .create().show();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this, PERMISSIONS,
                        RC_PERMISSION_ALL);
            }
        }
    }

    private void showAlerDialogForGPS() {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Enable Location !")
                .setMessage("Location must be enabled in settings in order to use this app." +
                        "Want to enable Location?")
                .setCancelable(false)
                .setPositiveButton("Yes, Go-to settings", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), RC_LOCAION_ON);
                    }
                })
                .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                        Toast.makeText(UserTypeActivity.this, "location is not enabled app will shut down shortly", Toast.LENGTH_SHORT).show();
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //Do something after 100ms
                                finish();
                            }
                        }, 2000);
                    }
                });
        android.app.AlertDialog alert = builder.create();
        alert.show();
    }

    private boolean isNetworkAvailable() {
        NetworkInfo networkInfo = ((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
