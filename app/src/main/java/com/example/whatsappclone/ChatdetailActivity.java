package com.example.whatsappclone;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.example.whatsappclone.utils.AESUtils;
import com.example.whatsappclone.utils.MyKeyStorage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;

import javax.crypto.SecretKey;

public class ChatdetailActivity extends AppCompatActivity {
    private static final String TAG = "ChatdetailActivity";
    ActivityChatdetailBinding binding;
    FirebaseDatabase database;
    FirebaseAuth auth;
    SecretKey aesKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityChatdetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        final String senderId = auth.getUid();
        String receiverId = getIntent().getStringExtra("userId");
        String userName = getIntent().getStringExtra("userName");
        String profilePic = getIntent().getStringExtra("profilePic");

        binding.userName.setText(userName);
        // Load profile image as before...

        binding.backArrow.setOnClickListener(v -> {
            startActivity(new Intent(ChatdetailActivity.this, MainActivity.class));
            finish();
        });

        aesKey = MyKeyStorage.loadAESKey(this);
        if (aesKey == null) {
            try {
                aesKey = AESUtils.generateKey();
                MyKeyStorage.saveAESKey(this, aesKey);
            } catch (Exception e) {
                Log.e(TAG, "Failed to generate encryption key", e);
                Toast.makeText(this, "Failed to generate encryption key", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        final ArrayList<MessageModel> messageModels = new ArrayList<>();
        final ChatAdapter chatAdapter = new ChatAdapter(messageModels, this, receiverId, aesKey);
        binding.chatRecycleView.setAdapter(chatAdapter);
        binding.chatRecycleView.setLayoutManager(new LinearLayoutManager(this));

        final String senderRoom = senderId + receiverId;
        final String receiverRoom = receiverId + senderId;

        database.getReference("chats").child(senderRoom)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messageModels.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            MessageModel model = snap.getValue(MessageModel.class);
                            if (model != null) {
                                model.setMessageId(snap.getKey());
                                try {
                                    String decryptedMsg = AESUtils.decrypt(model.getMessage(), aesKey);
                                    model.setMessage(decryptedMsg);
                                } catch (Exception e) {
                                    Log.e(TAG, "Decryption failed for message: " + model.getMessage(), e);
                                    model.setMessage("[Decryption failed]");
                                }
                                messageModels.add(model);
                            }
                        }
                        chatAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Database error: " + error.getMessage());
                    }
                });

        binding.send.setOnClickListener(v -> {
            String messageText = binding.enterMessage.getText().toString().trim();
            if (messageText.isEmpty()) {
                Toast.makeText(this, "Enter message", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                String encrypted = AESUtils.encrypt(messageText, aesKey);
                MessageModel message = new MessageModel(senderId, encrypted);
                message.setTimestamp(new Date().getTime());

                binding.enterMessage.setText("");

                database.getReference("chats").child(senderRoom).push().setValue(message)
                        .addOnSuccessListener(unused -> database.getReference("chats").child(receiverRoom).push().setValue(message))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to send message", e));

            } catch (Exception e) {
                Log.e(TAG, "Encryption failed", e);
                Toast.makeText(this, "Encryption failed", Toast.LENGTH_SHORT).show();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
