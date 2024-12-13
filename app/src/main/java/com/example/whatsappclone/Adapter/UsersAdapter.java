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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {
    ArrayList<Users> list;
    Context context;

    public UsersAdapter(Context context, ArrayList<Users> list) {
        this.context = context;
        this.list = list;
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

        // Load profile picture with a fallback
        if (users.getProfilePic() != null && !users.getProfilePic().isEmpty()) {
            Picasso.get()
                    .load(users.getProfilePic())
                    .placeholder(R.drawable.avatar3) // Placeholder image
                    .error(R.drawable.avatar3) // Error image if loading fails
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.avatar3); // Default image
        }

        // Set user details
        holder.userName.setText(users.getUserName());
        //this code is to set the last message by ordering the last time stamp
        FirebaseDatabase.getInstance().getReference().child("chats")
                .child(FirebaseAuth.getInstance().getUid() + users.getUserId())
                .orderByChild("timestamp")
                .limitToLast(1)
                .addValueEventListener(new ValueEventListener() { // Use addValueEventListener for real-time updates
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.hasChildren()) {
                            for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                                String message = snapshot1.child("message").getValue(String.class);
                                holder.lastMessage.setText(message);
                            }
                        } else {
                            holder.lastMessage.setText(""); // Show nothing if no messages are found
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("UsersAdapter", "Failed to fetch last message", error.toException());
                    }
                });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(context, ChatdetailActivity.class);
                intent.putExtra("userId", users.getUserId());
                intent.putExtra("profilePic",users.getProfilePic());
                intent.putExtra("userName",users.getUserName());
                context.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
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
