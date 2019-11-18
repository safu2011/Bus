package com.example.bus.InfoFragmentUi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.bus.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static com.example.bus.Services.ProducerService.customersList;

public class ConsumerInfoFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";
    private View root;
    private RecyclerView.Adapter<ConsumerInfoFragment.ViewHolderRt> adapter;
    private RecyclerView recyclerView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_customer_info, container, false);
        NetworkInfo networkInfo = ((ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            recyclerView();
            hideNoInternetScreen();
        }else{
            showNoInternetScreen();
        }
        return root;
    }

    private void recyclerView() {
        recyclerView = root.findViewById(R.id.rv_customers_info_fragment);
        recyclerView.setLayoutManager(new LinearLayoutManager(root.getContext()));
        adapter = new RecyclerView.Adapter<ConsumerInfoFragment.ViewHolderRt>() {
            @NonNull
            @Override
            public ConsumerInfoFragment.ViewHolderRt onCreateViewHolder(@NonNull ViewGroup viewGroup, int ViewType) {
                return new ViewHolderRt(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.customer_info_blueprint, viewGroup, false));
            }

            @Override
            public void onBindViewHolder(@NonNull ConsumerInfoFragment.ViewHolderRt viewHolderRt, final int i) {

                viewHolderRt.contact_name.setText(customersList.get(i).getCustomerName());
                viewHolderRt.number.setText(customersList.get(i).getCustomerPhoneNumber());
                viewHolderRt.parentConsumerInfo.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        if(isNetworkAvailable()){
                        DatabaseReference referenceCustomer = FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Customers").child(customersList.get(i).getId());
                        referenceCustomer.removeValue();

                        DatabaseReference referenceDriver = FirebaseDatabase.getInstance().getReference("Consumers List").child(customersList.get(i).getId()).child("Drivers").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                        referenceDriver.removeValue();

                        final DatabaseReference referenceOccupiedSeats = FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Seats Occupied");
                        referenceOccupiedSeats.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                int occupiedSeats = dataSnapshot.getValue(int.class);
                                occupiedSeats--;
                                referenceOccupiedSeats.setValue(occupiedSeats);

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                        customersList.remove(customersList.get(i));
                        adapter.notifyDataSetChanged();
                        }
                        return true;
                    }
                });

            }

            @Override
            public int getItemCount() {
                return customersList.size();
            }
        };
        recyclerView.setAdapter(adapter);
    }

    private boolean isNetworkAvailable() {
        NetworkInfo networkInfo = ((ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void showNoInternetScreen(){
        LinearLayout noInternet = root.findViewById(R.id.ly_no_internet_consumer_info_fragment);
        noInternet.setVisibility(View.VISIBLE);
    }

    private void hideNoInternetScreen(){
        LinearLayout noInternet = root.findViewById(R.id.ly_no_internet_consumer_info_fragment);
        noInternet.setVisibility(View.GONE);
    }

    private class ViewHolderRt extends RecyclerView.ViewHolder {
        TextView contact_name, number;
        LinearLayout parentConsumerInfo;

        public ViewHolderRt(@NonNull View itemView) {
            super(itemView);
            parentConsumerInfo = itemView.findViewById(R.id.ly_consumerInfo);
            contact_name = itemView.findViewById(R.id.tv_customer_name);
            number = itemView.findViewById(R.id.tv_customer_number);
        }

    }
}