package com.example.bus.Activities;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.appbar.AppBarLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

import com.example.bus.FragmentsAddNewDriver.AvailableDriversFragment;
import com.example.bus.FragmentsAddNewDriver.ChoosePickUpLocationFragment;
import com.example.bus.ModelClasses.DriverModelClass;
import com.example.bus.R;

import java.util.ArrayList;

public class AddNewDriverActivity extends AppCompatActivity {
    public static int INDEX = 0;
    public static ArrayList<Fragment> fragmentsList;
    public static ArrayList<Boolean> isFragmentAvailable;
    public static TextView APPBAR_TITLE;
    public static DriverModelClass CURRENT_SELECTED_DRIVER;
    private AppBarLayout appBarLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_driver);
        appBarLayout = findViewById(R.id.appbar_assessment_questionnaires);
        fragmentsList = new ArrayList<>();
        isFragmentAvailable = new ArrayList<>();
        APPBAR_TITLE = findViewById(R.id.tv_app_bar_title_add_new_driver);
        APPBAR_TITLE.setText("Choose Driver");
        creatFragments();
        setFragment(fragmentsList.get(INDEX));

    }

    private void creatFragments() {
        // creating fragments objects
        AvailableDriversFragment availableDriversFragment = new AvailableDriversFragment();
        ChoosePickUpLocationFragment choosePickUpLocationFragment = new ChoosePickUpLocationFragment();

        fragmentsList.add(availableDriversFragment);
        fragmentsList.add(choosePickUpLocationFragment);

        for (int i = 0; i < fragmentsList.size(); i++) {
            isFragmentAvailable.add(false);
        }
    }


    private void setFragment(Fragment fragment) {
        if (isFragmentAvailable.get(INDEX)) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container_add_new_driver, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.fragment_container_add_new_driver, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
            isFragmentAvailable.set(INDEX, true);
        }
    }


    @Override
    public void onBackPressed() {
        if (INDEX != 0) {
            INDEX--;
            setFragment(fragmentsList.get(INDEX));
        } else {
            startActivity(new Intent(AddNewDriverActivity.this, ConsumerActivity.class));
            finish();
        }
    }
}