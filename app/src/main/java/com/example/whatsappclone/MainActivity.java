package com.example.whatsappclone;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.whatsappclone.Adapter.Freagmentadapter;
import com.example.whatsappclone.databinding.ActivityMainBinding;
import com.example.whatsappclone.utils.RSAKeyManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.security.KeyPair;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Inflate the layout using binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // Ensure user has encryption keys
        ensureEncryptionKeys();

        // Set up the toolbar
        setSupportActionBar(binding.toolbar); // Directly set the toolbar using binding
        binding.viewpager.setAdapter(new Freagmentadapter(getSupportFragmentManager()));
        binding.tablelayout.setupWithViewPager(binding.viewpager);
        // Handle insets for system bars
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void ensureEncryptionKeys() {
        // Check if user has RSA keys
        if (!RSAKeyManager.hasKeys(this)) {
            try {
                Log.d(TAG, "No encryption keys found, generating new keys...");
                
                // Generate new RSA key pair
                KeyPair keyPair = RSAKeyManager.generateKeyPair();
                
                // Save private key locally
                RSAKeyManager.saveKeyPair(this, keyPair);
                
                // Save public key to Firebase
                String publicKeyStr = RSAKeyManager.publicKeyToString(keyPair.getPublic());
                String userId = mAuth.getCurrentUser().getUid();
                
                FirebaseDatabase.getInstance()
                    .getReference()
                    .child("PublicKeys")
                    .child(userId)
                    .setValue(publicKeyStr)
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "Encryption keys generated and saved successfully");
                        Toast.makeText(MainActivity.this, "Encryption setup complete", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save public key to Firebase", e);
                    });
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to generate encryption keys", e);
                Toast.makeText(this, "Failed to setup encryption. Please restart app.", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Encryption keys already exist");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { // Menu bar is activated i.e. settings, logout, group chat
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.settings) {
//            Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
            Intent intent=new Intent(MainActivity.this,SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.groupChat) {
            //Toast.makeText(this, "Group Chat is Clicked", Toast.LENGTH_SHORT).show();
            Intent intent1=new Intent(MainActivity.this,GroupChatActivity.class);
            startActivity(intent1);

            return true;
        } else if (item.getItemId() == R.id.log_out) {
            mAuth.signOut();
            Intent intent=new Intent(MainActivity.this,SignInActivity.class);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

}
