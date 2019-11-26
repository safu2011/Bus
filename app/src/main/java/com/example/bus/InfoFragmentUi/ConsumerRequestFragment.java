package com.example.bus.InfoFragmentUi;

import android.app.MediaRouteButton;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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

import com.example.bus.Activities.ViewPickUpPointLocation;
import com.example.bus.ModelClasses.CustomerModelClass;
import com.example.bus.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ConsumerRequestFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";
    private DatabaseReference myRef;
    private View root;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter<ViewHolderRt> adapter;
    private ArrayList<CustomerModelClass> newCustomersRequestsList;
    private LinearLayout lyNoRequests;
    private int seatsOccupied;
    private LinearLayout loadingScreen;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_customer_request, container, false);
        lyNoRequests = root.findViewById(R.id.ly_no_requests);
        loadingScreen = root.findViewById(R.id.ll_loading_requests_fragment);
        newCustomersRequestsList = new ArrayList<>();
        NetworkInfo networkInfo = ((ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            myRef = FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.child("Seats Occupied").exists())
                        seatsOccupied = dataSnapshot.child("Seats Occupied").getValue(int.class);
                    else
                        seatsOccupied = 0;

                    for (DataSnapshot ds : dataSnapshot.child("New Requests").getChildren()) {
                        String customerId = ds.getKey();
                        String customerName = ds.child("Customer Name").getValue(String.class);
                        String customerPhoneNumber = ds.child("Phone Number").getValue(String.class);
                        String customerLatti = ds.child("Pick up point latitude").getValue(Double.class).toString();
                        String customerLongi = ds.child("Pick up point longitude").getValue(Double.class).toString();
                        newCustomersRequestsList.add(new CustomerModelClass(customerId, customerName, customerPhoneNumber, customerLatti, customerLongi, null, false,0));
                        lyNoRequests.setVisibility(View.GONE);
                    }
                    hideNoInternetScreen();
                    hideLoadingScreen();
                    recyclerView();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(root.getContext(),"Opps something went wrong !!!",Toast.LENGTH_LONG).show();
                    hideLoadingScreen();
                }
            });

        }else{
            hideLoadingScreen();
            showNoInternetScreen();
            lyNoRequests.setVisibility(View.GONE);
        }
        Log.d("mylog", "onCreateView: " + newCustomersRequestsList.size());

        return root;
    }

    private void recyclerView() {
        recyclerView = root.findViewById(R.id.recycler_view_requests);
        recyclerView.setLayoutManager(new LinearLayoutManager(root.getContext()));
        adapter = new RecyclerView.Adapter<ConsumerRequestFragment.ViewHolderRt>() {
            @NonNull
            @Override
            public ConsumerRequestFragment.ViewHolderRt onCreateViewHolder(@NonNull ViewGroup viewGroup, int ViewType) {
                return new ConsumerRequestFragment.ViewHolderRt(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.new_customer_info_blueprint, viewGroup, false));
            }

            @Override
            public void onBindViewHolder(@NonNull ConsumerRequestFragment.ViewHolderRt viewHolderRt, final int i) {

                viewHolderRt.contact_name.setText(newCustomersRequestsList.get(i).getCustomerName());
                viewHolderRt.number.setText(newCustomersRequestsList.get(i).getCustomerPhoneNumber());

                viewHolderRt.btnViewPickUpPoint.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity(), ViewPickUpPointLocation.class);
                        intent.putExtra("selectedCustomer", newCustomersRequestsList.get(i));
                        intent.putExtra("seatsOccupied", seatsOccupied);
                        startActivity(intent);
                    }
                });

                viewHolderRt.btnAcceptRequest.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Customers").child(newCustomersRequestsList.get(i).getId());
                        ref.child("Name").setValue(newCustomersRequestsList.get(i).getCustomerName());
                        ref.child("Phone Number").setValue(newCustomersRequestsList.get(i).getCustomerPhoneNumber());
                        ref.child("Pick up point latitude").setValue(newCustomersRequestsList.get(i).getCustomerLatitude());
                        ref.child("Pick up point longitude").setValue(newCustomersRequestsList.get(i).getCustomerLongitude()).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                FirebaseDatabase.getInstance().getReference("Consumers List").child(newCustomersRequestsList.get(i).getId()).child("Drivers").child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .setValue("Service Active");
                                FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("New Requests")
                                        .child(newCustomersRequestsList.get(i).getId())
                                        .removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        seatsOccupied++;
                                        DatabaseReference capacityRef = FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Seats Occupied");
                                        capacityRef.setValue(seatsOccupied);

                                        newCustomersRequestsList.remove(i);
                                        adapter.notifyDataSetChanged();

                                    }
                                });
                            }
                        });

                    }
                });



            }

            @Override
            public int getItemCount() {
                return newCustomersRequestsList.size();
            }
        };
        recyclerView.setAdapter(adapter);
    }

    public void showLoadingScreen() {
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        loadingScreen.setVisibility(View.VISIBLE);
    }

    public void hideLoadingScreen() {
        loadingScreen.setVisibility(View.GONE);
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void showNoInternetScreen(){
        LinearLayout noInternet = root.findViewById(R.id.ly_no_internet_requests_fragment);
        noInternet.setVisibility(View.VISIBLE);
    }

    private void hideNoInternetScreen(){
        LinearLayout noInternet = root.findViewById(R.id.ly_no_internet_requests_fragment);
        noInternet.setVisibility(View.GONE);
    }

    private class ViewHolderRt extends RecyclerView.ViewHolder {
        TextView contact_name, number;
        Button btnViewPickUpPoint, btnAcceptRequest;
        ImageView dp;

        public ViewHolderRt(@NonNull View itemView) {
            super(itemView);
            dp = itemView.findViewById(R.id.imageview_customer);
            contact_name = itemView.findViewById(R.id.tv_new_customer_name);
            number = itemView.findViewById(R.id.tv_new_customer_number);
            btnViewPickUpPoint = itemView.findViewById(R.id.btn_view_pick_up_point);
            btnAcceptRequest = itemView.findViewById(R.id.btn_accept_request);
        }

    }
}