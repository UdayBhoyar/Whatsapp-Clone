package com.example.whatsappclone;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
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
import com.example.whatsappclone.databinding.ActivityGroupChatBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;

public class GroupChatActivity extends AppCompatActivity {

    ActivityGroupChatBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding=ActivityGroupChatBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        binding.backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(GroupChatActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });

        FirebaseDatabase database=FirebaseDatabase.getInstance();
        final ArrayList<MessageModel> messageModels=new ArrayList<>();

        final String senderId= FirebaseAuth.getInstance().getUid();
        binding.userName.setText("Group Chat");

        final ChatAdapter adapter=new ChatAdapter(this, messageModels);
        binding.chatRecycleView.setAdapter(adapter);

        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        binding.chatRecycleView.setLayoutManager(layoutManager);

        database.getReference().child("Group Chat").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageModels.clear();
                for(DataSnapshot dataSnapshot:snapshot.getChildren()){                      // showing all the available message from the database
                    MessageModel model=dataSnapshot.getValue(MessageModel.class);
                    messageModels.add(model);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        binding.send.setOnClickListener(new View.OnClickListener() {//the id is same for groupchat xml and other but with the help of binding we can differ it so it does cause confusion
            @Override
            public void onClick(View v) {
                final String message=binding.enterMessage.getText().toString();
                final MessageModel model=new MessageModel(senderId,message);
                model.setTimestamp(new Date().getTime());

                binding.enterMessage.setText(null);

                database.getReference().child("Group Chat").push().setValue(model).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                       // Toast.makeText(GroupChatActivity.this, "Message sent", Toast.LENGTH_SHORT).show();

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