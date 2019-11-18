package com.example.bus.Activities;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.bus.R;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rilixtech.CountryCodePicker;

import java.util.Collections;
import java.util.List;

public class SignUpActivity extends AppCompatActivity {
    private LinearLayout loadingScreen;
    private EditText etfirstName, etlastName, etphoneNumber, etDutyAtInstitutionName, etSeatingCapacity;
    private Spinner spinnerVehicalType, spinnerUserType;
    private Button btnProceed;
    private CountryCodePicker countryCodePicker;
    private DatabaseReference myRef;
    private LinearLayout lyProducerRelatedInfo;
    private String[] userTypeList;

    private String fullName, dutyAtInstitute, vehicalType, phoneNumber;
    private int seatingCapacity;

    private static final int RC_SIGN_UP = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        setContentView(R.layout.activity_sign_up);

        init();
        setAdapterForSpinner();

        btnProceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = null;
                if (connectivityManager != null) {
                    networkInfo = connectivityManager.getActiveNetworkInfo();
                }
                if (networkInfo != null && networkInfo.isConnected()) {
                    checkAllFields();
                } else {
                    Toast.makeText(SignUpActivity.this, "No Internet !", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void checkAllFields() {
        if (etfirstName.getText().toString().trim().equals("")) {
            etfirstName.setError("Enter First Name");
            return;
        }
        if (etlastName.getText().toString().trim().equals("")) {
            etlastName.setError("Enter Last Name");
            return;
        } else {
            fullName = etfirstName.getText().toString() + " " + etlastName.getText().toString();
        }
        if (lyProducerRelatedInfo.getVisibility() == View.VISIBLE) {
            if (etDutyAtInstitutionName.getText().toString().trim().equals("")) {
                etDutyAtInstitutionName.setError("Enter Institution Name");
                return;
            }else {
                dutyAtInstitute = etDutyAtInstitutionName.getText().toString();
            }

            String[] vehical_type_list = getResources().getStringArray(R.array.vehical_type);
            vehicalType = vehical_type_list[spinnerVehicalType.getSelectedItemPosition()];

            if(etSeatingCapacity.getText().toString().trim().equals("")){
                etSeatingCapacity.setError("Enter Capacity");
                return;
            }else{
                seatingCapacity = Integer.parseInt(etSeatingCapacity.getText().toString().trim());
            }
        }

        if (!etphoneNumber.getText().toString().trim().equals("")) {
            phoneNumber = "+" + countryCodePicker.getSelectedCountryCode() + etphoneNumber.getText().toString().trim();
            checkIfUserExsit(phoneNumber);
        } else {
            etphoneNumber.setError("Please Enter A Valid Number");
            return;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == RC_SIGN_UP) {//checking that if activity sesult is called for the signon intent
            //IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                if (lyProducerRelatedInfo.getVisibility() == View.VISIBLE) {
                    myRef = myRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                    myRef.child("Name").setValue(fullName);
                    myRef.child("Phone Number").setValue(phoneNumber);
                    myRef.child("Institute Name").setValue(dutyAtInstitute);
                    myRef.child("Vehical Type").setValue(vehicalType);
                    myRef.child("IsActive").setValue(false);
                    myRef.child("Capacity").setValue(seatingCapacity);
                    startActivity(new Intent(SignUpActivity.this, ProducerMapsActivity.class));
                } else {
                    myRef = myRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                    myRef.child("Name").setValue(fullName);
                    myRef.child("Phone Number").setValue(phoneNumber);
                    startActivity(new Intent(SignUpActivity.this, ConsumerActivity.class));
                }
                finish();

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

    private void init() {
        loadingScreen = findViewById(R.id.ll_loading_signup);
        etfirstName = findViewById(R.id.et_signup_first_name);
        etlastName = findViewById(R.id.et_signup_last_name);
        etphoneNumber = findViewById(R.id.et_signup_number);
        etDutyAtInstitutionName = findViewById(R.id.et_signup_duty_at_Institution);
        spinnerVehicalType = findViewById(R.id.spinner_signup);
        etSeatingCapacity = findViewById(R.id.et_signup_seating_capacity);
        spinnerUserType = findViewById(R.id.spinner_user_type);
        lyProducerRelatedInfo = findViewById(R.id.ly_producer_related);
        btnProceed = findViewById(R.id.btn_proceed_signup);
        countryCodePicker = findViewById(R.id.ccp_signup);
        userTypeList = getResources().getStringArray(R.array.user_type);
    }

    private void setAdapterForSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.vehical_type, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicalType.setAdapter(adapter);

        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(this, R.array.user_type, android.R.layout.select_dialog_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserType.setAdapter(adapter1);
        spinnerUserType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (userTypeList[i].equals("Producer"))
                    lyProducerRelatedInfo.setVisibility(View.VISIBLE);
                else
                    lyProducerRelatedInfo.setVisibility(View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void checkIfUserExsit(final String checkPhoneNumber) {
        showLoadingScreen();
        // Read from the database
        if (userTypeList[spinnerUserType.getSelectedItemPosition()].equals("Producer"))
            myRef = FirebaseDatabase.getInstance().getReference("Producers List");
        else
            myRef = FirebaseDatabase.getInstance().getReference("Consumers List");
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    String phoneNumber = ds.child("Phone Number").getValue(String.class);
                    if (checkPhoneNumber.equals(phoneNumber)) {
                        hideLoadingScreen();
                        etphoneNumber.setError("User with specifc Phone Number already exsits");
                        Toast.makeText(SignUpActivity.this, "User with specifc Phone Number already exsits !", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                hideLoadingScreen();
                verifyCode(checkPhoneNumber);
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
                RC_SIGN_UP);
    }

    private void showLoadingScreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        loadingScreen.setVisibility(View.VISIBLE);
    }

    private void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }
}
