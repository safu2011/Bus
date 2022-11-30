package com.example.bus.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bus.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AddDriverForAdminActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText etDriverId;
    private TextView tvName, tvNumber, tvInstituteName, tvVehicalType;
    private LinearLayout lyDriverInfo;
    private String driverId = "null";
    private ArrayList<String> myDriversIdList, institueNameList;
    private LinearLayout loadingScreen, lyDriverAdded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_driver_for_admin);



        etDriverId = findViewById(R.id.et_id_add_driver_admin);
        tvName = findViewById(R.id.tv_driver_name_add_driver_admin);
        tvNumber = findViewById(R.id.tv_driver_number_add_driver_admin);
        tvInstituteName = findViewById(R.id.tv_driver_duty_at_add_driver_admin);
        tvVehicalType = findViewById(R.id.tv_driver_vehical_add_driver_admin);
        lyDriverInfo = findViewById(R.id.ly_driver_info_add_driver_admin);
        loadingScreen = findViewById(R.id.ll_loading_admin_add_driver);
        lyDriverAdded = findViewById(R.id.ly_driver_added_add_driver_admin);
        findViewById(R.id.iv_search_add_driver_admin).setOnClickListener(this);
        findViewById(R.id.btn_add_driver_admin).setOnClickListener(this);
        myDriversIdList = new ArrayList<>();
        institueNameList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Admins List")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("Drivers");

        if(isNetworkAvailable()) {
            showLoadingScreen();
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            myDriversIdList.add(ds.getKey());
                        }
                        hideLoadingScreen();
                    } else {
                        hideLoadingScreen();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        else{
            Toast.makeText(AddDriverForAdminActivity.this, "No Internet !!!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.iv_search_add_driver_admin:
                if(isNetworkAvailable()){
                    if(!etDriverId.getText().toString().trim().equals("") && !myDriversIdList.contains(etDriverId.getText().toString().trim()) && !driverId.equals(etDriverId.getText().toString().trim())) {
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Producers List")
                                .child(etDriverId.getText().toString().trim());

                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                showLoadingScreen();
                                if (dataSnapshot.exists()) {
                                    driverId = etDriverId.getText().toString().trim();
                                    tvName.setText(dataSnapshot.child("Name").getValue(String.class));
                                    tvNumber.setText(dataSnapshot.child("Phone Number").getValue(String.class));

                                    for(DataSnapshot ds: dataSnapshot.child("Institute Name List").getChildren()){
                                        institueNameList.add(ds.getKey());
                                    }
                                    if(institueNameList.size()>1){
                                        tvInstituteName.setText("Show List");
                                        tvInstituteName.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                showInstitueListDialogBox(AddDriverForAdminActivity.this,institueNameList);
                                            }
                                        });
                                    }else{
                                        tvInstituteName.setText(institueNameList.get(0));
                                    }

                                    tvVehicalType.setText(dataSnapshot.child("Vehical Type").getValue(String.class));
                                    hideLoadingScreen();
                                    lyDriverInfo.setVisibility(View.VISIBLE);
                                    AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
                                    alphaAnimation.setDuration(500);
                                    lyDriverInfo.startAnimation(alphaAnimation);

                                } else {
                                    hideLoadingScreen();
                                    Toast.makeText(AddDriverForAdminActivity.this,"No Such User !!!",Toast.LENGTH_LONG).show();
                                    lyDriverInfo.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }else{
                        if(myDriversIdList.contains(etDriverId.getText().toString().trim()))
                            Toast.makeText(this,"Driver already added !!!",Toast.LENGTH_LONG).show();
                    }

                }else{
                    Toast.makeText(AddDriverForAdminActivity.this, "No Internet !!!", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.btn_add_driver_admin:
                if(isNetworkAvailable()) {
                    showLoadingScreen();
                    FirebaseDatabase.getInstance().getReference("Admins List")
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .child("Drivers")
                            .child(driverId)
                            .setValue("Service Active").addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            hideLoadingScreen();
                            lyDriverAdded.setVisibility(View.VISIBLE);
                            FirebaseDatabase.getInstance().getReference("Producers List")
                                    .child(driverId)
                                    .child("Admin")
                                    .setValue(FirebaseAuth.getInstance().getCurrentUser().getUid());
                            Toast.makeText(AddDriverForAdminActivity.this, "Driver Added", Toast.LENGTH_LONG).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            hideLoadingScreen();
                            Toast.makeText(AddDriverForAdminActivity.this, "Oops Something went wrong !!!", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                else{
                    Toast.makeText(AddDriverForAdminActivity.this, "No Internet !!!", Toast.LENGTH_LONG).show();
                }

                break;
        }
    }

    public void showLoadingScreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        loadingScreen.setVisibility(View.VISIBLE);
    }

    public void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private boolean isNetworkAvailable() {
        NetworkInfo networkInfo = ((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void showInstitueListDialogBox(Context context ,ArrayList<String> institueNameList){
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialogbox_institution_list);
        ListView listView = dialog.findViewById(R.id.lv_dialog_box_institute_list);

        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, institueNameList);

        listView.setAdapter(itemsAdapter);

        Button btnBack = dialog.findViewById(R.id.btn_back_dialog_box_institute_list);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();

            }
        });
        dialog.show();
    }

}
