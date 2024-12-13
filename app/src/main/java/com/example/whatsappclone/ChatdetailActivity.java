package com.example.whatsappclone;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.whatsappclone.Adapter.ChatAdapter;
import com.example.whatsappclone.Models.MessageModel;
import com.example.whatsappclone.databinding.ActivityChatdetailBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;

public class ChatdetailActivity extends AppCompatActivity {
    ActivityChatdetailBinding binding;
    FirebaseDatabase database;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding=ActivityChatdetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        database=FirebaseDatabase.getInstance();
        auth=FirebaseAuth.getInstance();
        final String senderId=auth.getUid();
        String receiveId=getIntent().getStringExtra("userId");
        String userName=getIntent().getStringExtra("userName");
        String profilePic=getIntent().getStringExtra("profilePic");

        binding.userName.setText(userName);//taking name and display in for specific chat when clicked on it

        Picasso.get().load(profilePic).placeholder(R.drawable.avatar3).into(binding.profileImage);//taking image from main page and displaying it in chat

        binding.backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(ChatdetailActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });
        final ArrayList<MessageModel> messageModels=new ArrayList<>();
        final ChatAdapter chatAdapter=new ChatAdapter(messageModels,this,receiveId);

        binding.chatRecycleView.setAdapter(chatAdapter);

        LinearLayoutManager layoutManager=new LinearLayoutManager(this);

        binding.chatRecycleView.setLayoutManager(layoutManager);

        final String senderRoom=senderId+receiveId; //by this we can identify from where did the message came from and where it is to be sent
        final String receiverRoom=receiveId+senderId;

        database.getReference().child("chats")
                        .child(senderRoom)
                                .addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            messageModels.clear();
                                            for(DataSnapshot snapshot1:snapshot.getChildren()){
                                                MessageModel model=snapshot1.getValue(MessageModel.class);
                                                model.setMessageId(snapshot1.getKey());
                                                messageModels.add(model);
                                            }
                                            chatAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });

        binding.send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message=binding.enterMessage.getText().toString();// taking msg from text field which is to be sent
                if (message.trim().isEmpty()) {
                    Toast.makeText(ChatdetailActivity.this, "Enter something", Toast.LENGTH_SHORT).show();
                    return; // Stop further execution
                }

                final MessageModel model=new MessageModel(senderId,message);
                model.setTimestamp(new Date().getTime());
                binding.enterMessage.setText("");

                database.getReference().child("chats") //we used 2 addonsuccesslistener one for sender and one for receiver now by identifying the ids we will identify who is receiver and who is sender
                        .child(senderRoom).push().setValue(model).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                database.getReference().child("chats")
                                        .child(receiverRoom)
                                        .push().setValue(model).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {

                                            }
                                        });
                            }
                        });
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}