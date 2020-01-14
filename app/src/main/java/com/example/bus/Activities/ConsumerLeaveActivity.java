package com.example.bus.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bus.ModelClasses.DriverModelClass;
import com.example.bus.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ConsumerLeaveActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ArrayList<DriverModelClass> driversList;
    private LinearLayout loadingScreen;
    private RecyclerView.Adapter<ViewHolderRt> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumer_leave);
        driversList = new ArrayList<>();
        recyclerView();
        showLoadingScreen();

        if(isNetworkAvailable()) {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Consumers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Drivers");
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            showLoadingScreen();
                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Producers List").child(ds.getKey());
                            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        String id = dataSnapshot.getKey();
                                        String name = dataSnapshot.child("Name").getValue(String.class);
                                        String phoneNumber = dataSnapshot.child("Phone Number").getValue(String.class);
                                        String vehicleType = dataSnapshot.child("Vehical Type").getValue(String.class);

                                        ArrayList<String> institueNameList = new ArrayList<>();
                                        for(DataSnapshot ds1: dataSnapshot.child("Institute Name List").getChildren()){
                                            institueNameList.add(ds1.getKey());
                                        }

                                        driversList.add(new DriverModelClass(id, name, phoneNumber, institueNameList, vehicleType,0,0));

                                        adapter.notifyDataSetChanged();
                                        hideLoadingScreen();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Toast.makeText(ConsumerLeaveActivity.this, "Opps Something went wrong !!!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }else{
                        Toast.makeText(ConsumerLeaveActivity.this,"Add Drivers First !!!",Toast.LENGTH_LONG).show();
                        hideLoadingScreen();
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(ConsumerLeaveActivity.this, "Opps Something went wrong !!!", Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            Toast.makeText(ConsumerLeaveActivity.this,"Check Internet Connection",Toast.LENGTH_SHORT).show();
        }
    }

    private void recyclerView() {
        recyclerView = findViewById(R.id.rv_drivers_info_Leave);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerView.Adapter<ConsumerLeaveActivity.ViewHolderRt>() {

            @Override
            public ConsumerLeaveActivity.ViewHolderRt onCreateViewHolder(ViewGroup viewGroup, int ViewType) {
                return new ConsumerLeaveActivity.ViewHolderRt(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.driver_info_leave_blueprint, viewGroup, false));
            }

            @Override
            public void onBindViewHolder(final ConsumerLeaveActivity.ViewHolderRt viewHolderRt, final int i) {

                viewHolderRt.driver_name.setText(driversList.get(i).getName());
                viewHolderRt.number.setText(driversList.get(i).getNumber());
                viewHolderRt.vehicleType.setText(driversList.get(i).getVehicleType());

                if(driversList.get(i).getDutyAt().size()>1){
                    viewHolderRt.dutyAt.setText("Show List");
                    viewHolderRt.dutyAt.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showInstitueListDialogBox(ConsumerLeaveActivity.this,driversList.get(i).getDutyAt());
                        }
                    });

                }else{
                    viewHolderRt.dutyAt.setText(driversList.get(i).getDutyAt().get(0));
                }


                viewHolderRt.ly_send_request.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(isNetworkAvailable()){
                            Intent intent = new Intent(ConsumerLeaveActivity.this, DaysOffActivity.class);
                            intent.putExtra("DriverId",driversList.get(i).getId());
                            startActivity(intent);
                        } else {
                            Toast.makeText(ConsumerLeaveActivity.this,"Check Internet Connection",Toast.LENGTH_SHORT).show();
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

    public void showLoadingScreen() {
        if (loadingScreen == null)
            loadingScreen = findViewById(R.id.ll_loading_drivers_info_Leave);
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

    private class ViewHolderRt extends RecyclerView.ViewHolder {
        TextView driver_name, number, dutyAt, vehicleType;
        LinearLayout ly_send_request;

        public ViewHolderRt(View itemView) {
            super(itemView);
            driver_name = itemView.findViewById(R.id.tv_leave_driver_name);
            number = itemView.findViewById(R.id.tv_leave_driver_number);
            dutyAt = itemView.findViewById(R.id.tv_driver_duty_at_leave);
            vehicleType = itemView.findViewById(R.id.tv_driver_leave_vehical);
            ly_send_request = itemView.findViewById(R.id.ly_leave_request_send);
        }

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
