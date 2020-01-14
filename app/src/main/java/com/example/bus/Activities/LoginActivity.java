package com.example.bus.Activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.bus.R;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.rilixtech.CountryCodePicker;

import java.util.Collections;
import java.util.List;


public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1000;
    private CustomReceiver receiver;
    private LinearLayout loadingScreen;
    private CountryCodePicker countryCodePicker;
    private SharedPreferences.Editor editor;
    private String userType = null;
    private String consumerId;
    private static final int RC_PERMISSION_ALL = 1212;
    private static final int RC_LOCAION_ON = 1211;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        setContentView(R.layout.activity_login);

        userType = getIntent().getStringExtra("user");

        final EditText tvPhoneNumber = findViewById(R.id.tv_phoneNumber);
        countryCodePicker = findViewById(R.id.ccp_login);
        loadingScreen = findViewById(R.id.ll_loading_loging);

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.user_type), Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        findViewById(R.id.btn_sign_in).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = null;
                if (connectivityManager != null) {
                    networkInfo = connectivityManager.getActiveNetworkInfo();
                }
                if (networkInfo != null && networkInfo.isConnected()) {
                    if (!tvPhoneNumber.getText().toString().trim().equals("")) {
                        String phoneNumber = "+" + countryCodePicker.getSelectedCountryCode() + tvPhoneNumber.getText().toString().trim();
                        checkIfUserExsit(phoneNumber);
                    } else
                        tvPhoneNumber.setError("Please Enter A Valid Number");
                } else {
                    Toast.makeText(LoginActivity.this, "No Internet !", Toast.LENGTH_LONG).show();
                }
            }
        });

        findViewById(R.id.tv_login_create_account).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        receiver = new CustomReceiver();
        registerReceiver(receiver, new IntentFilter("User Not Found In Database"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == RC_SIGN_IN) {//checking that if activity sesult is called for the signon intent
            //IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                if (userType.equalsIgnoreCase("driver")) {
                    editor.putString(getString(R.string.user_type), "Producer");
                    editor.commit();
                    Intent intent = new Intent(LoginActivity.this, ProducerMapsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("online", true);
                    startActivity(intent);
                } else if(userType.equalsIgnoreCase("admin")){
                    editor.putString(getString(R.string.user_type), "Admin");
                    editor.commit();
                    Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    editor.putString(getString(R.string.user_type), "Consumer");
                    editor.commit();
                    Intent intent = new Intent(LoginActivity.this, ConsumerActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("online", true);
                    String token = FirebaseInstanceId.getInstance().getToken();
                    Log.d("mylog", "onActivityResult: "+token);
                    FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            FirebaseDatabase.getInstance().getReference("Consumers List").child(consumerId).child("Messaging Token").setValue( task.getResult().getToken() );
                        }
                    });
                    startActivity(intent);
                }

            }else if (requestCode == RC_LOCAION_ON) {
                LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Toast.makeText(this, "location is not enabled app will shut down shortly", Toast.LENGTH_SHORT).show();
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 100ms
                            finish();
                        }
                    }, 2000);
                } else {
                    recreate();
                }

            } else {
                // IdpResponse response = IdpResponse.fromResultIntent(data);
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                Toast.makeText(this, "Something went wrong please Try again", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void checkIfUserExsit(final String checkPhoneNumber) {
        showLoadingScreen();
        DatabaseReference myRef;
        // Read from the database

        if (userType.equalsIgnoreCase("driver"))
            myRef = FirebaseDatabase.getInstance().getReference("Producers List");
        else if(userType.equalsIgnoreCase("parent"))
            myRef = FirebaseDatabase.getInstance().getReference("Consumers List");
        else
            myRef = FirebaseDatabase.getInstance().getReference("Admins List");

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    String phoneNumber = ds.child("Phone Number").getValue(String.class);
                    if (checkPhoneNumber.equals(phoneNumber)) {
                        consumerId = ds.getKey();
                        hideLoadingScreen();
                        verifyCode(phoneNumber);
                        return;
                    }
                }
                hideLoadingScreen();
                sendBroadcast(new Intent("User Not Found In Database"));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read valu
            }
        });
    }

    private void verifyCode(String phoneNumber) {
        List<AuthUI.IdpConfig> providers = Collections.singletonList(
                new AuthUI.IdpConfig.PhoneBuilder().setDefaultNumber(phoneNumber).build());

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
    }

    private void showLoadingScreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        loadingScreen.setVisibility(View.VISIBLE);
    }

    private void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == RC_PERMISSION_ALL) {
            if (permissions.length > 0 && permissions[0].equals(android.Manifest.permission.READ_EXTERNAL_STORAGE) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate();
            } else if (grantResults.length > 0 &&/* permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION) && */grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate();
//                    mLocationPermissionGranted = true;
            } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Permissions denied the app will shut down shortly", Toast.LENGTH_LONG).show();
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 2000);
            }
        }

    }


    public class CustomReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals("User Not Found In Database")) {
                Toast.makeText(LoginActivity.this, "No User Found !", Toast.LENGTH_LONG).show();
            }
        }
    }
}
