package com.example.bus.InfoFragmentUi;


import android.content.Context;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.bus.Activities.ProducerMapsActivity;
import com.example.bus.ModelClasses.DriverModelClass;
import com.example.bus.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import static com.example.bus.Services.ProducerService.customersList;


public class ProviderPersonalInfoFragment extends Fragment {

    private View root;
    private LinearLayout loadingScreen, lyPersonalInfo, lyEditPersonalInfo;
    private ImageView userImage, userImageUpdate;
    private TextView tvName, tvPhoneNumner, tvDutyAt, tvVehicalType, tvTotalCustomers, tvTotalCustomersUpdate, tvPhoneNumberUpdate;
    private EditText etName, etDutyAt;
    private Spinner spinnerVehicalType;
    private DriverModelClass myDriver;
    private FloatingActionButton fbEditPersonalInfo, fbUpdatePersonalInfo;
    private String[] vehicalTypeList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_personal_info, container, false);
        init();
        showLoadingScreen();
        setListeners();
        getDp(R.drawable.pic);
        NetworkInfo networkInfo = ((ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            DatabaseReference myref = FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            myref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        String name = dataSnapshot.child("Name").getValue(String.class);
                        String phone_number = dataSnapshot.child("Phone Number").getValue(String.class);
                        String vehical_type = dataSnapshot.child("Vehical Type").getValue(String.class);
                        String duty_at = dataSnapshot.child("Institute Name").getValue(String.class);

                        myDriver = new DriverModelClass(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                                name,
                                phone_number,
                                duty_at,
                                vehical_type,
                                0,
                                0);

                        setValues();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    hideLoadingScreen();
                    Toast.makeText(root.getContext(),"Opps Something went wrong !!!",Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(root.getContext(), ProducerMapsActivity.class));
                }
            });

        }else{
            showNoInternetScreen();
            hideLoadingScreen();
        }
        return root;
    }

    private void init(){
        loadingScreen = root.findViewById(R.id.ll_loading_personal_info_fragment);
        lyPersonalInfo = root.findViewById(R.id.ly_personal_info);
        lyEditPersonalInfo = root.findViewById(R.id.ly_personal_info_edit);
        userImage = root.findViewById(R.id.iv_user_image_personal_image);
        userImageUpdate = root.findViewById(R.id.iv_user_image_personal_image_update);
        tvName = root.findViewById(R.id.tv_name_personal_info);
        tvPhoneNumner = root.findViewById(R.id.tv_phoneNumber_personal_info);
        tvPhoneNumberUpdate = root.findViewById(R.id.tv_phoneNumber_personal_info_update);
        tvDutyAt = root.findViewById(R.id.tv_duty_at_personal_info);
        tvVehicalType = root.findViewById(R.id.tv_drives_personal_info);
        tvTotalCustomers = root.findViewById(R.id.tv_total_customers_personal_info);
        tvTotalCustomersUpdate = root.findViewById(R.id.tv_total_customers_personal_info_update);
        etName = root.findViewById(R.id.et_name_personal_info);
        etDutyAt = root.findViewById(R.id.et_duty_at_personal_info);
        spinnerVehicalType = root.findViewById(R.id.spinner_personal_info);
        fbEditPersonalInfo = root.findViewById(R.id.fb_edit_personal_info);
        fbUpdatePersonalInfo = root.findViewById(R.id.fb_update_personal_info);


        vehicalTypeList = getResources().getStringArray(R.array.vehical_type);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(root.getContext(),
                R.array.vehical_type, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicalType.setAdapter(adapter);
    }

    private void setValues(){
        tvName.setText(myDriver.getName());
        etName.setText(myDriver.getName());

        tvPhoneNumner.setText(myDriver.getNumber());
        tvPhoneNumberUpdate.setText(myDriver.getNumber());

        tvDutyAt.setText(myDriver.getDutyAt());
        etDutyAt.setText(myDriver.getDutyAt());

        tvVehicalType.setText(myDriver.getVehicleType());

        for (int i=0 ; i<vehicalTypeList.length ; i++){
            String value = vehicalTypeList[i];
            if(value.equals(tvVehicalType.getText())){
                spinnerVehicalType.setSelection(i);
            }
        }

        tvTotalCustomers.setText(customersList.size()+"");
        tvTotalCustomersUpdate.setText(customersList.size()+"");

        hideLoadingScreen();
    }
    private void setListeners(){
        fbEditPersonalInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lyPersonalInfo.setVisibility(View.GONE);
                lyEditPersonalInfo.setVisibility(View.VISIBLE);
                fbEditPersonalInfo.hide();
                fbUpdatePersonalInfo.show();
            }
        });

        fbUpdatePersonalInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!etName.getText().toString().trim().equals("") && !etDutyAt.getText().toString().trim().equals("")){
                    if(isNetworkAvailable()){
                        String name = etName.getText().toString();
                        String vehicleType = vehicalTypeList[spinnerVehicalType.getSelectedItemPosition()];
                        String dutyAt = etDutyAt.getText().toString();
                        setNewValues(name, vehicleType, dutyAt);
                        lyPersonalInfo.setVisibility(View.VISIBLE);
                        lyEditPersonalInfo.setVisibility(View.GONE);
                        fbEditPersonalInfo.show();
                        fbUpdatePersonalInfo.hide();
                    }else{
                        Toast.makeText(root.getContext(),"Please check Internet !",Toast.LENGTH_LONG).show();
                        lyPersonalInfo.setVisibility(View.VISIBLE);
                        lyEditPersonalInfo.setVisibility(View.GONE);
                        fbEditPersonalInfo.show();
                        fbUpdatePersonalInfo.hide();
                    }
                }
                else {
                    Toast.makeText(root.getContext(),"Fill up fields first !",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setNewValues(final String name, final String vehicleType, final String dutyAt){
        showLoadingScreen();
        DatabaseReference myref = FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
       myref.child("Name").setValue(name);
       myref.child("Vehical Type").setValue(vehicleType);
       myref.child("Institute Name").setValue(dutyAt).addOnCompleteListener(new OnCompleteListener<Void>() {
           @Override
           public void onComplete(@NonNull Task<Void> task) {
               hideLoadingScreen();
               tvName.setText(name);
               tvVehicalType.setText(vehicleType);
               tvDutyAt.setText(dutyAt);
           }
       });


    }

    private void getDp(int id){
        Glide
                .with(this)
                .load(id)
                .into(userImage);
        Glide
                .with(this)
                .load(id)
                .into(userImageUpdate);
    }

    private boolean isNetworkAvailable() {
        NetworkInfo networkInfo = ((ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void showNoInternetScreen(){
        LinearLayout noInternet = root.findViewById(R.id.ly_no_internet_personal_info_fragment);
        noInternet.setVisibility(View.VISIBLE);
    }

    private void hideNoInternetScreen(){
        LinearLayout noInternet = root.findViewById(R.id.ly_no_internet_personal_info_fragment);
        noInternet.setVisibility(View.GONE);
    }

    public void showLoadingScreen() {
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        loadingScreen.setVisibility(View.VISIBLE);
    }

    public void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }
}
