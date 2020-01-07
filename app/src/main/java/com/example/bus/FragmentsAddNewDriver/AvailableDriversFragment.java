package com.example.bus.FragmentsAddNewDriver;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.bus.ModelClasses.DriverModelClass;
import com.example.bus.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import static com.example.bus.Activities.AddNewDriverActivity.CURRENT_SELECTED_DRIVER;
import static com.example.bus.Activities.AddNewDriverActivity.INDEX;
import static com.example.bus.Activities.AddNewDriverActivity.fragmentsList;
import static com.example.bus.Activities.AddNewDriverActivity.isFragmentAvailable;
import static com.example.bus.FragmentsAddNewDriver.ChoosePickUpLocationFragment.IS_RUNNING_FIRST_TIME;

public class AvailableDriversFragment extends Fragment {

    private View root;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter<ViewHolderRt> adapter;
    private ArrayList<DriverModelClass> driversList;
    private LinearLayout loadingScreen;
    private EditText etDriverID;
    private ImageView ivSearchButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_available_drivers, container, false);
        etDriverID = root.findViewById(R.id.et_id_add_driver);
        ivSearchButton = root.findViewById(R.id.iv_search_button_add_driver);
        driversList = new ArrayList<>();
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("Producers List");

        ivSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id = etDriverID.getText().toString().trim();
                if(!id.equals("")){
                    DatabaseReference newRef = myRef.child(id);
                    newRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                int vehicleOccupiedSeats = dataSnapshot.child("Seats Occupied").getValue(int.class);
                                int vehicalCapacity = dataSnapshot.child("Capacity").getValue(int.class);
                                int seatsAvailable = vehicalCapacity - vehicleOccupiedSeats;
                                if (vehicalCapacity - vehicleOccupiedSeats > 0) {
                                    String id = dataSnapshot.getKey();
                                    String name = dataSnapshot.child("Name").getValue(String.class);
                                    String phoneNumber = dataSnapshot.child("Phone Number").getValue(String.class);

                                    ArrayList<String> institueNameList = new ArrayList<>();
                                    for(DataSnapshot ds: dataSnapshot.child("Institute Name List").getChildren()){
                                        institueNameList.add(ds.getKey());
                                    }

                                    String vehicalType = dataSnapshot.child("Vehical Type").getValue(String.class);

                                    driversList.clear();
                                    driversList.add(new DriverModelClass(id, name, phoneNumber, institueNameList, vehicalType, vehicalCapacity, seatsAvailable));
                                    adapter.notifyDataSetChanged();
                                }else{
                                    Toast.makeText(root.getContext(),"The driver vehicle is fully booked",Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }else{
                    etDriverID.setError("Please enter Driver Id");
                }
            }
        });


        showLoadingScreen();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    int vehicleOccupiedSeats = ds.child("Seats Occupied").getValue(int.class);
                    int vehicalCapacity =ds.child("Capacity").getValue(int.class);
                    int seatsAvailable = vehicalCapacity - vehicleOccupiedSeats;
                    if(vehicalCapacity-vehicleOccupiedSeats > 0) {
                        String id = ds.getKey();
                        String name = ds.child("Name").getValue(String.class);
                        String phoneNumber = ds.child("Phone Number").getValue(String.class);
                        ArrayList<String> institueNameList = new ArrayList<>();
                        for(DataSnapshot ds1: ds.child("Institute Name List").getChildren()){
                            institueNameList.add(ds1.getKey());
                        }
                        String vehicalType = ds.child("Vehical Type").getValue(String.class);

                        driversList.add(new DriverModelClass(id, name, phoneNumber, institueNameList, vehicalType, vehicalCapacity,seatsAvailable));

                    }
                }
                recyclerView();
                hideLoadingScreen();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                hideLoadingScreen();
                Toast.makeText(root.getContext(), "Opps Something Went Wrong !!!", Toast.LENGTH_SHORT).show();
            }
        });
        return root;
    }

    private void recyclerView() {
        recyclerView = root.findViewById(R.id.rv_available_drivers_fragment);
        recyclerView.setLayoutManager(new LinearLayoutManager(root.getContext()));
        adapter = new RecyclerView.Adapter<AvailableDriversFragment.ViewHolderRt>() {

            @Override
            public AvailableDriversFragment.ViewHolderRt onCreateViewHolder(ViewGroup viewGroup, int ViewType) {
                return new AvailableDriversFragment.ViewHolderRt(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.add_new_driver_info_blueprint, viewGroup, false));
            }

            @Override
            public void onBindViewHolder(AvailableDriversFragment.ViewHolderRt viewHolderRt, final int i) {

                viewHolderRt.driver_name.setText(driversList.get(i).getName());
                viewHolderRt.number.setText(driversList.get(i).getNumber());
                if(driversList.get(i).getDutyAt().size()>1){
                    viewHolderRt.dutyAt.setText("Show List");
                    viewHolderRt.dutyAt.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showInstitueListDialogBox(root.getContext(),driversList.get(i).getDutyAt());
                        }
                    });

                }else{
                    viewHolderRt.dutyAt.setText(driversList.get(i).getDutyAt().get(0));
                }

                viewHolderRt.vehicleType.setText(driversList.get(i).getVehicleType());
                viewHolderRt.vehicleSeatsAvailable.setText(driversList.get(i).getSeatsAvailable()+"");

                viewHolderRt.parentLy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        CURRENT_SELECTED_DRIVER = driversList.get(i);
                        INDEX++;
                        if (isFragmentAvailable.get(INDEX)) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container_add_new_driver, fragmentsList.get(INDEX))
                                    .addToBackStack(null)
                                    .commit();
                            IS_RUNNING_FIRST_TIME = true;

                        } else {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .add(R.id.fragment_container_add_new_driver, fragmentsList.get(INDEX))
                                    .addToBackStack(null)
                                    .commit();
                            isFragmentAvailable.set(INDEX, true);
                        }
                    }
                });

            }

            @Override
            public int getItemCount() {
                return driversList.size();
            }
        };
        recyclerView.setAdapter(adapter);
    }


    private class ViewHolderRt extends RecyclerView.ViewHolder {
        LinearLayout parentLy;
        TextView driver_name, number, dutyAt, vehicleType, vehicleSeatsAvailable;

        public ViewHolderRt(View itemView) {
            super(itemView);
            parentLy = itemView.findViewById(R.id.ly_drivers_info_blueprint);
            driver_name = itemView.findViewById(R.id.tv_driver_name);
            number = itemView.findViewById(R.id.tv_driver_number);
            dutyAt = itemView.findViewById(R.id.tv_driver_duty_at);
            vehicleType = itemView.findViewById(R.id.tv_driver_vehical);
            vehicleSeatsAvailable = itemView.findViewById(R.id.tv_add_driver_seats_available);
        }

    }

    public void showLoadingScreen() {
        if (loadingScreen == null)
            loadingScreen = root.findViewById(R.id.ll_loading_available_drivers);
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        loadingScreen.setVisibility(View.VISIBLE);
    }

    public void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void showInstitueListDialogBox(Context context , ArrayList<String> institueNameList){
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
