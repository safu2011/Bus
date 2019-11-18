package com.example.bus.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bus.ModelClasses.DriverModelClass;
import com.example.bus.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class DriversInfoActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RecyclerView.Adapter<DriversInfoActivity.ViewHolderRt> adapter;
    private ArrayList<DriverModelClass> driversList;
    private LinearLayout loadingScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drivers);
        driversList = new ArrayList<>();
        recyclerView();
        showLoadingScreen();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Consumers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Drivers");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    showLoadingScreen();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Producers List").child(ds.getKey());
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            int vehicleOccupiedSeats = dataSnapshot.child("Seats Occupied").getValue(int.class);
                            int vehicalCapacity =dataSnapshot.child("Capacity").getValue(int.class);
                            int seatsAvailable = vehicalCapacity - vehicleOccupiedSeats;
                            String id = dataSnapshot.getKey();
                            String name = dataSnapshot.child("Name").getValue(String.class);
                            String phoneNumber = dataSnapshot.child("Phone Number").getValue(String.class);
                            String dutyAt = dataSnapshot.child("Institute Name").getValue(String.class);
                            String vehicalType = dataSnapshot.child("Vehical Type").getValue(String.class);

                            driversList.add(new DriverModelClass(id, name, phoneNumber, dutyAt, vehicalType, vehicalCapacity,seatsAvailable));
                            adapter.notifyDataSetChanged();
                            hideLoadingScreen();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(DriversInfoActivity.this, "Opps Something went wrong !!!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(DriversInfoActivity.this, "Opps Something went wrong !!!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void recyclerView() {
        recyclerView = findViewById(R.id.rv_drivers_info);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerView.Adapter<DriversInfoActivity.ViewHolderRt>() {

            @Override
            public DriversInfoActivity.ViewHolderRt onCreateViewHolder(ViewGroup viewGroup, int ViewType) {
                return new DriversInfoActivity.ViewHolderRt(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.driver_info_blueprint, viewGroup, false));
            }

            @Override
            public void onBindViewHolder(DriversInfoActivity.ViewHolderRt viewHolderRt, final int i) {

                viewHolderRt.driver_name.setText(driversList.get(i).getName());
                viewHolderRt.number.setText(driversList.get(i).getNumber());
                viewHolderRt.dutyAt.setText(driversList.get(i).getDutyAt());
                viewHolderRt.vehicalType.setText(driversList.get(i).getVehicleType());
                viewHolderRt.vehicleCapacity.setText(driversList.get(i).getSeatsAvailable()+"");

                viewHolderRt.callDriver.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialContactPhone(driversList.get(i).getNumber());
                    }
                });
                viewHolderRt.removeDriver.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                       removeDriver(i);
                    }
                });
                viewHolderRt.messageDriver.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sendSms(driversList.get(i).getNumber());
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
        TextView driver_name, number, dutyAt, vehicalType, removeDriver, messageDriver, callDriver, vehicleCapacity;

        public ViewHolderRt(View itemView) {
            super(itemView);
            parentLy = itemView.findViewById(R.id.ly_drivers_info_blueprint);
            driver_name = itemView.findViewById(R.id.tv_driver_name);
            number = itemView.findViewById(R.id.tv_driver_number);
            dutyAt = itemView.findViewById(R.id.tv_driver_duty_at);
            vehicalType = itemView.findViewById(R.id.tv_driver_vehical);
            removeDriver = itemView.findViewById(R.id.tv_driver_remove);
            messageDriver = itemView.findViewById(R.id.tv_driver_message);
            callDriver = itemView.findViewById(R.id.tv_driver_call);
            vehicleCapacity = itemView.findViewById(R.id.tv_driver_seats_available);
        }

    }

    public void showLoadingScreen() {
        if (loadingScreen == null)
            loadingScreen = findViewById(R.id.ll_loading_drivers_info);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        loadingScreen.setVisibility(View.VISIBLE);
    }

    public void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void dialContactPhone(String phoneNumber) {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNumber, null)));
    }

    private void sendSms(String phoneNumber){
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("smsto:"));
        i.putExtra("address", phoneNumber);
        startActivity(Intent.createChooser(i, "Send sms via:"));
    }

    private void removeDriver(final int index){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Are you sure ?")
                .setTitle("Remove Driver")
                .setPositiveButton("Yes, Proceed ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        driversList.remove(index);
                        adapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();


    }
}
