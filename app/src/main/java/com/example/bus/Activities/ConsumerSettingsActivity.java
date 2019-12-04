package com.example.bus.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.example.bus.Fragments.NotificationHintFragment;
import com.example.bus.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ConsumerSettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnReduceValue , btnIncreaseValue, btnNotificaionHint, btnPreviousFragment, btnNextFragment;
    private EditText etDistanceValue;
    private Switch switchNotificaion;
    private int distanceValue;
    private ArrayList<String> allDriversIdList;
    private SharedPreferences.Editor editor;
    private ArrayList<Fragment> fragmentsList;
    private LinearLayout lyNotificationHintBg, lyMainNotificationHint;

    private String DILOG_BOX_TAG="DilogFragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumer_settings);
        btnReduceValue = findViewById(R.id.btn_customer_settings_reduce_distance);
        btnIncreaseValue = findViewById(R.id.btn_customer_settings_increase_distance);
        btnNotificaionHint = findViewById(R.id.btn_customer_settings_notification_hint);
        etDistanceValue = findViewById(R.id.et_customer_settings);
        switchNotificaion = findViewById(R.id.switch_customer_settings);
        lyNotificationHintBg = findViewById(R.id.ly_notification_setting_hint_bg);
        lyMainNotificationHint = findViewById(R.id.ly_main_notification_setting_hint);
        btnPreviousFragment = findViewById(R.id.btn_left_notification_hint);
        btnNextFragment = findViewById(R.id.btn_right_notification_hint);
        fragmentsList = new ArrayList<>();
        allDriversIdList = new ArrayList<>();

        fragmentsList.add(new NotificationHintFragment(R.drawable.ss_aggresive_notification,"Aggressive Notification"));
        fragmentsList.add(new NotificationHintFragment(R.drawable.ss_normal_notification,"Normal Notification"));

        btnIncreaseValue.setOnClickListener(this);
        btnReduceValue.setOnClickListener(this);
        btnNotificaionHint.setOnClickListener(this);
        btnPreviousFragment.setOnClickListener(this);
        btnNextFragment.setOnClickListener(this);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        editor = pref.edit();

        switchNotificaion.setChecked(pref.getBoolean("Aggressive Notification",false));
        distanceValue =  pref.getInt("distance",50);
        etDistanceValue.setText(String.valueOf(distanceValue));

        switchNotificaion.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    editor.putBoolean("Aggressive Notification",isChecked);
                Log.d("mylog", "onCheckedChanged: "+isChecked);
            }
        });

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

        if(lyNotificationHintBg.getVisibility() == View.GONE){
            if(distanceValue<30){
                Toast.makeText(ConsumerSettingsActivity.this,"Min limit is 30",Toast.LENGTH_SHORT).show();
            }else{
                if(isNetworkAvailable()){
                    for(int i=0; i<allDriversIdList.size() ;i++){
                        DatabaseReference notificationValueRef = FirebaseDatabase.getInstance().getReference("Producers List").child(allDriversIdList.get(i)).child("Customers").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Notification distance values");
                        notificationValueRef.setValue(distanceValue);
                        editor.putInt("distance",distanceValue);
                        editor.commit();
                    }

                }else{
                    Toast.makeText(this,"Please Check your internet !",Toast.LENGTH_LONG).show();
                }
                super.onBackPressed();
            }
        }
        else {
            lyNotificationHintBg.setVisibility(View.GONE);
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
                dialogBox();
                break;
            case R.id.btn_left_notification_hint:
                FrameLayout frameLayout = findViewById(R.id.framelayout_notification_hint);

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.framelayout_notification_hint, fragmentsList.get(0));
                frameLayout.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),
                        R.anim.fui_slide_out_left));

                transaction.addToBackStack(null);
                transaction.commit();
                break;
            case R.id.btn_right_notification_hint:
                FrameLayout frameLayout1 = findViewById(R.id.framelayout_notification_hint);
                FragmentTransaction transaction2 = getSupportFragmentManager().beginTransaction();

                transaction2.replace(R.id.framelayout_notification_hint, fragmentsList.get(1));

                frameLayout1.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),
                        R.anim.fui_slide_in_right));
                transaction2.addToBackStack(null);
                transaction2.commit();
                break;
        }
    }


    private void dialogBox(){
        lyNotificationHintBg.setVisibility(View.VISIBLE);
        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.slide);
        lyMainNotificationHint.startAnimation(animation);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.framelayout_notification_hint, fragmentsList.get(0));
        transaction.addToBackStack(null);
        transaction.commit();
    }
    private boolean isNetworkAvailable() {
        NetworkInfo networkInfo = ((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
