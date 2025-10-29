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
import com.example.whatsappclone.utils.HybridEncryption;
import com.example.whatsappclone.utils.RSAKeyManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;

public class ChatdetailActivity extends AppCompatActivity {
    private static final String TAG = "ChatdetailActivity";
    ActivityChatdetailBinding binding;
    FirebaseDatabase database;
    FirebaseAuth auth;
    PrivateKey myPrivateKey;
    PublicKey recipientPublicKey;

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

        // Load my private key (for decryption)
        myPrivateKey = RSAKeyManager.loadPrivateKey(this);
        if (myPrivateKey == null) {
            Log.e(TAG, "Failed to load private key");
            
            // Try to generate keys if they don't exist
            try {
                Log.d(TAG, "Attempting to generate new encryption keys...");
                KeyPair keyPair = RSAKeyManager.generateKeyPair();
                RSAKeyManager.saveKeyPair(this, keyPair);
                myPrivateKey = keyPair.getPrivate();
                
                // Save public key to Firebase
                String publicKeyStr = RSAKeyManager.publicKeyToString(keyPair.getPublic());
                String currentUserId = auth.getUid();
                database.getReference("PublicKeys").child(currentUserId).setValue(publicKeyStr)
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "New encryption keys generated successfully");
                        Toast.makeText(this, "Encryption setup complete. Please try again.", Toast.LENGTH_SHORT).show();
                    });
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to generate encryption keys", e);
                Toast.makeText(this, "Encryption setup failed. Please restart the app and try again.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        } else {
            Log.d(TAG, "Private key loaded successfully");
        }

        // Fetch recipient's public key from Firebase (for encryption)
        database.getReference("PublicKeys").child(receiverId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String publicKeyStr = snapshot.getValue(String.class);
                        if (publicKeyStr != null) {
                            try {
                                recipientPublicKey = RSAKeyManager.stringToPublicKey(publicKeyStr);
                                Log.d(TAG, "Recipient's public key loaded successfully");
                                Toast.makeText(ChatdetailActivity.this, "Secure chat ready", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to load recipient's public key", e);
                                Toast.makeText(ChatdetailActivity.this, "Failed to load encryption key", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.w(TAG, "Recipient has no public key in database - recipient needs to open the app");
                            Toast.makeText(ChatdetailActivity.this, 
                                "The recipient needs to open the app first to enable encrypted messaging. You can still send messages once they do.", 
                                Toast.LENGTH_LONG).show();
                            
                            // Keep checking for recipient's key every 3 seconds
                            retryFetchingRecipientKey(receiverId, 0);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to fetch recipient's public key", error.toException());
                        Toast.makeText(ChatdetailActivity.this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
                    }
                });

        final ArrayList<MessageModel> messageModels = new ArrayList<>();
        final ChatAdapter chatAdapter = new ChatAdapter(messageModels, this, receiverId, myPrivateKey);
        binding.chatRecycleView.setAdapter(chatAdapter);
        binding.chatRecycleView.setLayoutManager(new LinearLayoutManager(this));

        final String senderRoom = senderId + receiverId;
        final String receiverRoom = receiverId + senderId;

        // Listen for messages and decrypt them
        database.getReference("chats").child(senderRoom)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messageModels.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            MessageModel model = snap.getValue(MessageModel.class);
                            if (model != null) {
                                model.setMessageId(snap.getKey());
                                
                                // Decrypt the message using hybrid decryption
                                try {
                                    if (model.getMessage() != null && model.getEncryptedSessionKey() != null) {
                                        HybridEncryption.EncryptedMessage encMsg = 
                                            new HybridEncryption.EncryptedMessage(
                                                model.getMessage(), 
                                                model.getEncryptedSessionKey()
                                            );
                                        
                                        String decrypted = HybridEncryption.decrypt(encMsg, myPrivateKey);
                                        model.setMessage(decrypted);
                                    } else {
                                        // Handle old unencrypted messages or missing keys
                                        model.setMessage("[Encryption data missing]");
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Decryption failed for message", e);
                                    model.setMessage("[Decryption failed]");
                                }
                                
                                messageModels.add(model);
                            }
                        }
                        chatAdapter.notifyDataSetChanged();
                        
                        // Scroll to latest message
                        if (messageModels.size() > 0) {
                            binding.chatRecycleView.smoothScrollToPosition(messageModels.size() - 1);
                        }
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

            if (recipientPublicKey == null) {
                Toast.makeText(this, "Waiting for recipient to be online. Ask them to open the app first.", Toast.LENGTH_LONG).show();
                Log.d(TAG, "Cannot send message - recipient public key not available");
                return;
            }

            try {
                // Encrypt twice: one copy for recipient (with recipient's public key)
                // and one copy for the sender (with sender's own public key) so the
                // sender can decrypt their sent message in their own room.

                // Encrypt for recipient
                HybridEncryption.EncryptedMessage encForRecipient =
                        HybridEncryption.encrypt(messageText, recipientPublicKey);

                // Encrypt for sender (self)
                PublicKey myPublicKey = RSAKeyManager.loadPublicKey(this);
                if (myPublicKey == null) {
                    Log.e(TAG, "Sender public key missing while sending message");
                    Toast.makeText(this, "Encryption not initialized. Please reopen the app.", Toast.LENGTH_LONG).show();
                    return;
                }
                HybridEncryption.EncryptedMessage encForSender =
                        HybridEncryption.encrypt(messageText, myPublicKey);

                // Build message models
                MessageModel messageForSender = new MessageModel(
                        senderId,
                        encForSender.getEncryptedData(),
                        encForSender.getEncryptedSessionKey()
                );
                messageForSender.setTimestamp(new Date().getTime());

                MessageModel messageForReceiver = new MessageModel(
                        senderId,
                        encForRecipient.getEncryptedData(),
                        encForRecipient.getEncryptedSessionKey()
                );
                messageForReceiver.setTimestamp(messageForSender.getTimestamp());

                binding.enterMessage.setText("");

                // Save encrypted copies to respective rooms
                database.getReference("chats").child(senderRoom).push().setValue(messageForSender)
                        .addOnSuccessListener(unused -> {
                            database.getReference("chats").child(receiverRoom).push().setValue(messageForReceiver)
                                    .addOnSuccessListener(u2 -> Log.d(TAG, "Message sent and encrypted successfully"))
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to send message to receiver room", e);
                                        Toast.makeText(ChatdetailActivity.this, "Failed to deliver to recipient", Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to send message", e);
                            Toast.makeText(ChatdetailActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                        });

            } catch (Exception e) {
                Log.e(TAG, "Encryption failed", e);
                Toast.makeText(this, "Encryption failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Retry fetching recipient's public key with exponential backoff
     * This helps when the recipient hasn't opened the app yet
     */
    private void retryFetchingRecipientKey(String receiverId, int attemptCount) {
        if (attemptCount >= 10) {
            Log.d(TAG, "Stopped retrying to fetch recipient's key after 10 attempts");
            return;
        }

        // Wait before retrying (3 seconds * attempt number for backoff)
        new android.os.Handler().postDelayed(() -> {
            database.getReference("PublicKeys").child(receiverId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String publicKeyStr = snapshot.getValue(String.class);
                            if (publicKeyStr != null) {
                                try {
                                    recipientPublicKey = RSAKeyManager.stringToPublicKey(publicKeyStr);
                                    Log.d(TAG, "Recipient's public key loaded successfully on retry " + attemptCount);
                                    Toast.makeText(ChatdetailActivity.this, 
                                        "✓ Recipient is now online. You can send encrypted messages!", 
                                        Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed to parse recipient's public key", e);
                                }
                            } else {
                                // Key still not available, retry again
                                Log.d(TAG, "Recipient key not found, retry attempt " + (attemptCount + 1));
                                retryFetchingRecipientKey(receiverId, attemptCount + 1);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error fetching recipient key on retry", error.toException());
                        }
                    });
        }, 3000 + (attemptCount * 1000)); // Exponential backoff
    }
}
