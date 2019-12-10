package com.example.bus.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bus.ModelClasses.DriverModelClass;
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


public class DriversInfoActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RecyclerView.Adapter<DriversInfoActivity.ViewHolderRt> adapter;
    private ArrayList<DriverModelClass> driversList;
    private LinearLayout loadingScreen;
    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drivers);
        driversList = new ArrayList<>();
        recyclerView();
        showLoadingScreen();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("Consumers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Drivers");
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
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
                                int rating = 0;
                                if(dataSnapshot.child("Customers").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Driver rating").exists())
                                    rating = dataSnapshot.child("Customers").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Driver rating").getValue(int.class);
                                DriverModelClass driver = new DriverModelClass(id, name, phoneNumber, dutyAt, vehicalType, vehicalCapacity,seatsAvailable);
                                driver.setRating(rating);
                                driversList.add(driver);

                                adapter.notifyDataSetChanged();
                                hideLoadingScreen();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(DriversInfoActivity.this, "Opps Something went wrong !!!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }else{
                    hideLoadingScreen();
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
                viewHolderRt.rateDriver.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        rateDriver(i);
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
        TextView driver_name, number, dutyAt, vehicalType, removeDriver, messageDriver, callDriver, vehicleCapacity, rateDriver;

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
            rateDriver = itemView.findViewById(R.id.tv_driver_ratting);
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

    private boolean isNetworkAvailable() {
        NetworkInfo networkInfo = ((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
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
                        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("Producers List").child(driversList.get(index).getId());
                        DatabaseReference ref1 = rootRef.child("Customers").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                        if(isNetworkAvailable()){
                            showLoadingScreen();
                            ref1.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    DatabaseReference ref2 = FirebaseDatabase.getInstance().getReference("Consumers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Drivers").child(driversList.get(index).getId());
                                    ref2.removeValue();

                                    int occupiedSeats = driversList.get(index).getVehicleCapacity()-driversList.get(index).getSeatsAvailable();
                                    occupiedSeats--;
                                    rootRef.child("Seats Occupied").setValue(occupiedSeats);

                                    driversList.remove(index);
                                    adapter.notifyDataSetChanged();

                                    hideLoadingScreen();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    hideLoadingScreen();
                                    Toast.makeText(DriversInfoActivity.this,"Opps Something went wrong",Toast.LENGTH_LONG).show();
                                }
                            });
                        }else{
                            Toast.makeText(DriversInfoActivity.this,"No Internet !!!",Toast.LENGTH_LONG).show();
                        }
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

    private void rateDriver(int index){
        dialog = new Dialog(DriversInfoActivity.this);
        dialog.setContentView(R.layout.driver_ratting_blueprint);
        Button cancelDialog = dialog.findViewById(R.id.btn_cancel_rating);
        Button submitRating = dialog.findViewById(R.id.btn_submit_rating);

        setDriverRating(index);
        setRatingDialogBoxListeners(index);

        cancelDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        submitRating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isNetworkAvailable()) {
                    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("Producers List").child(driversList.get(index).getId()).child("Customers").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Driver rating");
                    rootRef.setValue(driversList.get(index).getRating());
                    Toast.makeText(DriversInfoActivity.this,"Rating submitted",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(DriversInfoActivity.this,"No Internet",Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();

            }
        });
        dialog.show();
    }

    private void setDriverRating(int index) {
        int value = driversList.get(index).getRating();
        switch (value){
            case 1:
                setFirstStar("null",index);
                break;
            case 2:
                setFirstStar("null",index);
                setSecondStar("null",index);
                break;
            case 3:
                setFirstStar("null",index);
                setSecondStar("null",index);
                setThirdStar("null",index);
                break;
            case 4:
                setFirstStar("null",index);
                setSecondStar("null",index);
                setThirdStar("null",index);
                setFourthStar("null",index);
                break;
            case 5:
                setFirstStar("null",index);
                setSecondStar("null",index);
                setThirdStar("null",index);
                setFourthStar("null",index);
                setFifthStar("null",index);
                break;
        }
    }

    private void setRatingDialogBoxListeners(int index){
        ImageView star1 = dialog.findViewById(R.id.unselected_star_1);
        ImageView star2 = dialog.findViewById(R.id.unselected_star_2);
        ImageView star3 = dialog.findViewById(R.id.unselected_star_3);
        ImageView star4 = dialog.findViewById(R.id.unselected_star_4);
        ImageView star5 = dialog.findViewById(R.id.unselected_star_5);

        ImageView selectedStar1 = dialog.findViewById(R.id.selected_star_1);
        ImageView selectedStar2 = dialog.findViewById(R.id.selected_star_2);
        ImageView selectedStar3 = dialog.findViewById(R.id.selected_star_3);
        ImageView selectedStar4 = dialog.findViewById(R.id.selected_star_4);
        ImageView selectedStar5 = dialog.findViewById(R.id.selected_star_5);

        star1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFirstStar("select",index);
            }
        });

        star2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFirstStar("select",index);
                setSecondStar("select",index);
            }
        });

        star3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFirstStar("select",index);
                setSecondStar("select",index);
                setThirdStar("select",index);
            }
        });

        star4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFirstStar("select",index);
                setSecondStar("select",index);
                setThirdStar("select",index);
                setFourthStar("select",index);
            }
        });

        star5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFirstStar("select",index);
                setSecondStar("select",index);
                setThirdStar("select",index);
                setFourthStar("select",index);
                setFifthStar("select",index);
            }
        });


        selectedStar1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFirstStar("unSelect",index);
            }
        });

        selectedStar2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFirstStar("unSelect",index);
                setSecondStar("unSelect",index);
            }
        });

        selectedStar3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFirstStar("unSelect",index);
                setSecondStar("unSelect",index);
                setThirdStar("unSelect",index);
            }
        });

        selectedStar4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFirstStar("unSelect",index);
                setSecondStar("unSelect",index);
                setThirdStar("unSelect",index);
                setFourthStar("unSelect",index);
            }
        });

        selectedStar5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFirstStar("unSelect",index);
                setSecondStar("unSelect",index);
                setThirdStar("unSelect",index);
                setFourthStar("unSelect",index);
                setFifthStar("unSelect",index);
            }
        });
    }

    private void clearAllStars(int index){
        setFirstStar("unSelect",index);
        setSecondStar("unSelect",index);
        setThirdStar("unSelect",index);
        setFourthStar("unSelect",index);
        setFifthStar("unSelect",index);
    }

    private void setFirstStar(String sate, int index){
        ImageView selectedStar1 = dialog.findViewById(R.id.selected_star_1);
        int value = driversList.get(index).getRating();
        if(sate.equals("select") && selectedStar1.getVisibility()==View.INVISIBLE){
            selectedStar1.setVisibility(View.VISIBLE);
            driversList.get(index).setRating(value+1);
        }else if(sate.equals("unSelect") && selectedStar1.getVisibility()==View.VISIBLE){
            selectedStar1.setVisibility(View.INVISIBLE);
            driversList.get(index).setRating(value-1);
        }else if(sate.equals("null")){
            selectedStar1.setVisibility(View.VISIBLE);
        }
    }

    private void setSecondStar(String sate, int index){
        ImageView selectedStar2 = dialog.findViewById(R.id.selected_star_2);
        int value = driversList.get(index).getRating();
        if(sate.equals("select") && selectedStar2.getVisibility()==View.INVISIBLE){
            selectedStar2.setVisibility(View.VISIBLE);
            driversList.get(index).setRating(value+1);
        }else if(sate.equals("unSelect") && selectedStar2.getVisibility()==View.VISIBLE){
            selectedStar2.setVisibility(View.INVISIBLE);
            driversList.get(index).setRating(value-1);
        }else if(sate.equals("null")){
            selectedStar2.setVisibility(View.VISIBLE);
        }
    }

    private void setThirdStar(String sate, int index){
        ImageView selectedStar3 = dialog.findViewById(R.id.selected_star_3);
        int value = driversList.get(index).getRating();
        if(sate.equals("select") && selectedStar3.getVisibility()==View.INVISIBLE){
            selectedStar3.setVisibility(View.VISIBLE);
            driversList.get(index).setRating(value + 1);
        }else if(sate.equals("unSelect") && selectedStar3.getVisibility()==View.VISIBLE){
            selectedStar3.setVisibility(View.INVISIBLE);
            driversList.get(index).setRating(value-1);
        }else if(sate.equals("null")){
            selectedStar3.setVisibility(View.VISIBLE);
        }
    }

    private void setFourthStar(String sate, int index){
        ImageView selectedStar4 = dialog.findViewById(R.id.selected_star_4);
        int value = driversList.get(index).getRating();
        if(sate.equals("select") && selectedStar4.getVisibility()==View.INVISIBLE){
            selectedStar4.setVisibility(View.VISIBLE);
            driversList.get(index).setRating(value + 1);
        }else if(sate.equals("unSelect") && selectedStar4.getVisibility()==View.VISIBLE){
            selectedStar4.setVisibility(View.INVISIBLE);
            driversList.get(index).setRating(value - 1);
        }else if(sate.equals("null")){
            selectedStar4.setVisibility(View.VISIBLE);
        }
    }

    private void setFifthStar(String sate, int index){
        ImageView selectedStar5 = dialog.findViewById(R.id.selected_star_5);
        int value = driversList.get(index).getRating();
        if(sate.equals("select") && selectedStar5.getVisibility()==View.INVISIBLE){
            selectedStar5.setVisibility(View.VISIBLE);
            driversList.get(index).setRating(value + 1);
        }else if(sate.equals("unSelect") && selectedStar5.getVisibility()==View.VISIBLE){
            selectedStar5.setVisibility(View.INVISIBLE);
            driversList.get(index).setRating(value - 1);
        }else if(sate.equals("null")){
            selectedStar5.setVisibility(View.VISIBLE);
        }
    }
}
