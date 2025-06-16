package com.example.whatsappclone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
import com.example.whatsappclone.utils.AESUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;

import javax.crypto.SecretKey;

public class GroupChatActivity extends AppCompatActivity {

    ActivityGroupChatBinding binding;
    private static final String TAG = "GroupChatActivity";
    private static final String PREFS_NAME = "GroupChatPrefs";
    private static final String GROUP_AES_KEY = "group_aes_key";
    private SecretKey groupAESKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityGroupChatBinding.inflate(getLayoutInflater());

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        // Initialize AES key for group chat
        initializeGroupAESKey();

        binding.backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GroupChatActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final ArrayList<MessageModel> messageModels = new ArrayList<>();

        final String senderId = FirebaseAuth.getInstance().getUid();
        binding.userName.setText("Group Chat");

        final ChatAdapter adapter = new ChatAdapter(this, messageModels);
        binding.chatRecycleView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.chatRecycleView.setLayoutManager(layoutManager);

        database.getReference().child("Group Chat").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageModels.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    MessageModel model = dataSnapshot.getValue(MessageModel.class);
                    if (model != null) {
                        // Decrypt the message before adding to list
                        try {
                            String decryptedMessage = AESUtils.decrypt(model.getMessage(), groupAESKey);
                            model.setMessage(decryptedMessage);
                            messageModels.add(model);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to decrypt message: " + e.getMessage());
                            // If decryption fails, it might be a plain text message (backward compatibility)
                            // or the message is corrupted, skip it or handle accordingly
                            if (isPlainText(model.getMessage())) {
                                messageModels.add(model); // Add as plain text
                                Log.w(TAG, "Message appears to be plain text, adding without decryption");
                            } else {
                                Log.e(TAG, "Skipping corrupted message");
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });

        binding.send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String message = binding.enterMessage.getText().toString().trim();

                if (message.isEmpty()) {
                    Toast.makeText(GroupChatActivity.this, "Please enter a message", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    // Encrypt the message before sending
                    String encryptedMessage = AESUtils.encrypt(message, groupAESKey);

                    final MessageModel model = new MessageModel(senderId, encryptedMessage);
                    model.setTimestamp(new Date().getTime());

                    binding.enterMessage.setText("");

                    database.getReference().child("Group Chat").push().setValue(model).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d(TAG, "Encrypted message sent successfully");
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Failed to encrypt message: " + e.getMessage());
                    Toast.makeText(GroupChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Initialize or retrieve the AES key for group chat encryption
     */
    private void initializeGroupAESKey() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedKey = prefs.getString(GROUP_AES_KEY, null);

        if (savedKey != null) {
            // Load existing key
            try {
                groupAESKey = AESUtils.stringToKey(savedKey);
                Log.d(TAG, "Loaded existing AES key for group chat");
            } catch (Exception e) {
                Log.e(TAG, "Failed to load existing AES key, generating new one", e);
                generateAndSaveNewKey(prefs);
            }
        } else {
            // Generate new key
            generateAndSaveNewKey(prefs);
        }
    }

    /**
     * Generate a new AES key and save it to SharedPreferences
     */
    private void generateAndSaveNewKey(SharedPreferences prefs) {
        try {
            groupAESKey = AESUtils.generateKey();
            String keyString = AESUtils.keyToString(groupAESKey);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(GROUP_AES_KEY, keyString);
            editor.apply();

            Log.d(TAG, "Generated and saved new AES key for group chat");
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate AES key", e);
            Toast.makeText(this, "Failed to initialize encryption", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Check if a string appears to be plain text (not encrypted)
     * This is a simple heuristic - you might want to improve this
     */
    private boolean isPlainText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // Check if it contains only printable characters and common punctuation
        // This is a basic check - encrypted data usually contains non-printable characters
        return text.matches("^[\\p{Print}\\p{Space}]*$") &&
                !text.contains("=") && // Base64 usually contains =
                text.length() < 200; // Encrypted messages are usually longer
    }

    /**
     * Method to reset the group encryption key (call this if needed)
     */
    public void resetGroupEncryptionKey() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(GROUP_AES_KEY);
        editor.apply();

        generateAndSaveNewKey(prefs);
        Toast.makeText(this, "Group encryption key reset", Toast.LENGTH_SHORT).show();
    }
}