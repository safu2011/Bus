package com.example.bus.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.bus.R;

public class NotificationHintFragment extends Fragment {
    private View root;
    private ImageView imageView;
    private TextView textView;
    private int Id;
    private String text;

    public NotificationHintFragment(int id, String text) {
        this.Id = id;
        this.text = text;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.blueprint_notification_hint_pictures, container, false);
        imageView = root.findViewById(R.id.iv_notification_hint);
        textView = root.findViewById(R.id.tv_notification_hint);
        textView.setText(text);
        Glide
                .with(this)
                .load(Id)
                .into(imageView);

        return root;
    }
}
