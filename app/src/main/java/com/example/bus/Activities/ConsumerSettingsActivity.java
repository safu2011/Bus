package com.example.bus.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.example.bus.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ConsumerSettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnReduceValue , btnIncreaseValue, btnNotificaionHint;
    private EditText etDistanceValue;
    private Switch switchNotificaion;
    private int distanceValue;
    private ArrayList<String> allDriversIdList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumer_settings);
        btnReduceValue = findViewById(R.id.btn_customer_settings_reduce_distance);
        btnIncreaseValue = findViewById(R.id.btn_customer_settings_increase_distance);
        btnNotificaionHint = findViewById(R.id.btn_customer_settings_notification_hint);
        etDistanceValue = findViewById(R.id.et_customer_settings);
        switchNotificaion = findViewById(R.id.switch_customer_settings);
        allDriversIdList = new ArrayList<>();

        btnIncreaseValue.setOnClickListener(this);
        btnReduceValue.setOnClickListener(this);
        btnNotificaionHint.setOnClickListener(this);

        etDistanceValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(!charSequence.toString().trim().equals("")){
                    int tempValue = Integer.parseInt(charSequence.toString());
                    if(tempValue > 1000){
                        etDistanceValue.setText("1000");
                        Toast.makeText(ConsumerSettingsActivity.this,"Max limit is 1000",Toast.LENGTH_SHORT).show();
                    }else{
                        distanceValue = tempValue;
                    }
                }

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


        if(isNetworkAvailable()){

            DatabaseReference driversRef = FirebaseDatabase.getInstance().getReference("Consumers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Drivers");
            driversRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        for (DataSnapshot ds : dataSnapshot.getChildren()){
                            allDriversIdList.add(ds.getKey());
                        }
                        DatabaseReference notificationValueRef = FirebaseDatabase.getInstance().getReference("Producers List").child(allDriversIdList.get(0)).child("Customers").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Notification distance values");
                        notificationValueRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()){
                                    distanceValue = dataSnapshot.getValue(int.class);
                                    etDistanceValue.setText(String.valueOf(distanceValue));
                                }else {
                                    distanceValue = 50;
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });



        }else{
            distanceValue = 50;
            Toast.makeText(this,"Please Check your internet !",Toast.LENGTH_LONG).show();
        }


    }


    @Override
    public void onBackPressed() {

        if(distanceValue<30){
            Toast.makeText(ConsumerSettingsActivity.this,"Min limit is 30",Toast.LENGTH_SHORT).show();
        }else{
            if(isNetworkAvailable()){
                for(int i=0; i<allDriversIdList.size() ;i++){
                    DatabaseReference notificationValueRef = FirebaseDatabase.getInstance().getReference("Producers List").child(allDriversIdList.get(i)).child("Customers").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Notification distance values");
                    notificationValueRef.setValue(distanceValue);
                }

            }else{
                Toast.makeText(this,"Please Check your internet !",Toast.LENGTH_LONG).show();
            }
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_customer_settings_reduce_distance:
                if(distanceValue>30){
                    distanceValue--;
                    etDistanceValue.setText(String.valueOf(distanceValue));
                }else{
                    etDistanceValue.setText("30");
                    Toast.makeText(getBaseContext(),"Limit Reached",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_customer_settings_increase_distance:
                if(distanceValue<1000){
                    distanceValue++;
                    etDistanceValue.setText(String.valueOf(distanceValue));
                }else{
                    Toast.makeText(getBaseContext(),"Limit Reached",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_customer_settings_notification_hint:
                Toast.makeText(getBaseContext(),"distance = "+ distanceValue,Toast.LENGTH_SHORT).show();
                break;
        }
    }
    private boolean isNetworkAvailable() {
        NetworkInfo networkInfo = ((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
