package com.example.whatsappclone.Fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.whatsappclone.Adapter.UsersAdapter;
import com.example.whatsappclone.Models.Users;
import com.example.whatsappclone.R;
import com.example.whatsappclone.databinding.FragmentChatsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ChatsFragment extends Fragment {

    private static final String TAG = "ChatsFragment";

    public ChatsFragment() {
    }

    private FragmentChatsBinding binding;
    private ArrayList<Users> list = new ArrayList<>();
    private FirebaseDatabase database;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            // Inflate the layout for this fragment
            binding = FragmentChatsBinding.inflate(inflater, container, false);
            database = FirebaseDatabase.getInstance();

            // Set up RecyclerView with UsersAdapter
            UsersAdapter adapter = new UsersAdapter(getContext(), list);
            Log.d(TAG, "List size before setting adapter: " + (list != null ? list.size() : "null"));
            binding.chatRecycleView.setAdapter(adapter);

            // Set up RecyclerView layout manager
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            binding.chatRecycleView.setLayoutManager(layoutManager);

            // Fetch users from Firebase Database
            database.getReference().child("Users").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    list.clear(); // Clear the list to avoid duplicates
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        try {
                            Users user = dataSnapshot.getValue(Users.class);
                            if (user != null) {
                                user.setUserId(dataSnapshot.getKey());
                                if(!user.getUserId().equals(FirebaseAuth.getInstance().getUid())) { // for not including yourself into chat can be removed to message yourself
                                    list.add(user); // Add user to the list
                                }
                                Log.d(TAG, "User added: " + user.getUserName()); // Log user data
                            } else {
                                Log.e(TAG, "User data is null for key: " + dataSnapshot.getKey());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing user data", e);
                        }
                    }
                    Log.d(TAG, "Total users fetched: " + list.size());
                    adapter.notifyDataSetChanged(); // Notify the adapter of data changes
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Database error: " + error.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error initializing fragment", e);
        }

        return binding != null ? binding.getRoot() : null;
    }
}
