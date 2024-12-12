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

        // Ensure that 'lastMessage' field is not null before setting it
        String lastMessage = users.getLastMessage() != null ? users.getLastMessage() : "Last Message";
        holder.lastMessage.setText(lastMessage); // Set last message

        // Debug logging for checking data binding
        Log.d("UsersAdapter", "User: " + users.getUserName() + ", LastMessage: " + lastMessage);
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
