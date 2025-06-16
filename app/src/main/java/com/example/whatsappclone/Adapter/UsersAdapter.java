package com.example.whatsappclone.Adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.whatsappclone.ChatdetailActivity;
import com.example.whatsappclone.Models.Users;
import com.example.whatsappclone.R;
import com.example.whatsappclone.utils.AESUtils;
import com.example.whatsappclone.utils.MyKeyStorage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import javax.crypto.SecretKey;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {
    ArrayList<Users> list;
    Context context;
    private SecretKey secretKey;

    public UsersAdapter(Context context, ArrayList<Users> list) {
        this.context = context;
        this.list = list;

        // Load dynamic key from MyKeyStorage
        try {
            secretKey = MyKeyStorage.loadAESKey(context);
        } catch (Exception e) {
            Log.e("UsersAdapter", "Failed to load AES key", e);
            secretKey = null;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.sample_show_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Users users = list.get(position);

        // Load profile picture with fallback image
        if (users.getProfilePic() != null && !users.getProfilePic().isEmpty()) {
            Picasso.get()
                    .load(users.getProfilePic())
                    .placeholder(R.drawable.avatar3)
                    .error(R.drawable.avatar3)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.avatar3);
        }

        holder.userName.setText(users.getUserName());

        // Fetch last encrypted message from Firebase, decrypt it and set
        FirebaseDatabase.getInstance().getReference().child("chats")
                .child(FirebaseAuth.getInstance().getUid() + users.getUserId())
                .orderByChild("timestamp")
                .limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.hasChildren()) {
                            for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                                String encryptedMessage = snapshot1.child("message").getValue(String.class);
                                if (encryptedMessage != null) {
                                    if (AESUtils.isValidEncryptedData(encryptedMessage) && secretKey != null) {
                                        try {
                                            String decryptedMessage = AESUtils.decrypt(encryptedMessage, secretKey);
                                            holder.lastMessage.setText(decryptedMessage);
                                        } catch (Exception e) {
                                            Log.e("UsersAdapter", "Decryption failed", e);
                                            holder.lastMessage.setText("[Encrypted message]");
                                        }
                                    } else {
                                        holder.lastMessage.setText(encryptedMessage);
                                    }
                                } else {
                                    holder.lastMessage.setText("");
                                }
                            }
                        } else {
                            holder.lastMessage.setText("");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("UsersAdapter", "Failed to fetch last message", error.toException());
                        holder.lastMessage.setText("");
                    }
                });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatdetailActivity.class);
            intent.putExtra("userId", users.getUserId());
            intent.putExtra("profilePic", users.getProfilePic());
            intent.putExtra("userName", users.getUserName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView userName, lastMessage;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.profilePic);
            userName = itemView.findViewById(R.id.userNamelist);
            lastMessage = itemView.findViewById(R.id.lastMessage);
        }
    }
}