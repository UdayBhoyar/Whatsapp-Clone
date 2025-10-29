package com.example.whatsappclone;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.whatsappclone.databinding.ActivitySignInBinding;
import com.example.whatsappclone.utils.RSAKeyManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.security.KeyPair;

import static android.content.ContentValues.TAG;

public class SignInActivity extends AppCompatActivity {
    ActivitySignInBinding binding;
    FirebaseAuth mAuth;
    ProgressDialog progressDialog;
    GoogleSignInClient mGoogleSignInClient;
    private static final int REQ_SIGN_IN = 65;  // Unique request code for Google Sign-In

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        FirebaseApp.initializeApp(this);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Progress Dialog Setup
        progressDialog = new ProgressDialog(SignInActivity.this);
        progressDialog.setTitle("Login");
        progressDialog.setMessage("Please wait, validation in progress");

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id)) // Replace with your client_id in strings.xml
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Email/Password Sign-In
        binding.btnSignIn.setOnClickListener(v -> {
            String email = binding.txtemail.getText().toString().trim();
            String password = binding.password.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignInActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            progressDialog.show();

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            try {
                                // Check if user has RSA keys (for existing users who signed up before encryption)
                                if (!RSAKeyManager.hasKeys(SignInActivity.this)) {
                                    // Generate keys for existing users
                                    KeyPair keyPair = RSAKeyManager.generateKeyPair();
                                    RSAKeyManager.saveKeyPair(SignInActivity.this, keyPair);
                                    
                                    String publicKeyStr = RSAKeyManager.publicKeyToString(keyPair.getPublic());
                                    String userId = task.getResult().getUser().getUid();
                                    FirebaseDatabase.getInstance().getReference().child("PublicKeys").child(userId).setValue(publicKeyStr);
                                    
                                    Log.d("SignInActivity", "Generated RSA keys for existing user");
                                }
                                
                                Toast.makeText(SignInActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(SignInActivity.this, MainActivity.class));
                                finish();
                                
                            } catch (Exception e) {
                                Log.e("SignInActivity", "Failed to setup encryption keys", e);
                                Toast.makeText(SignInActivity.this, "Login successful but encryption setup failed", Toast.LENGTH_SHORT).show();
                                
                                // Still allow login
                                startActivity(new Intent(SignInActivity.this, MainActivity.class));
                                finish();
                            }
                        } else {
                            Toast.makeText(SignInActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Redirect to Sign-Up
        binding.clicktosignnup.setOnClickListener(v -> {
            startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
        });

        // Google Sign-In
        binding.googlebutton.setOnClickListener(v -> signInWithGoogle());

        // Automatically redirect if user is already logged in
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(SignInActivity.this, MainActivity.class));
            finish();
        }
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, REQ_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle: " + account.getIdToken());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign-in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToDatabase(user);
                            Toast.makeText(SignInActivity.this, "Google Login Successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignInActivity.this, MainActivity.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(SignInActivity.this, "Google Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToDatabase(FirebaseUser user) {
        // Get a reference to the Realtime Database
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        // Prepare user data to match your database structure
        Map<String, Object> userData = new HashMap<>();
        userData.put("mail", user.getEmail());
        userData.put("password", ""); // Password is not provided in Google Sign-In
        userData.put("status", "Hey there! I am using this app."); // Default status
        userData.put("userName", user.getDisplayName()); // Use display name as userName

        // Save the data under the user's unique UID
        databaseReference.child(user.getUid()).setValue(userData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User added to database"))
                .addOnFailureListener(e -> Log.w(TAG, "Error adding user to database", e));
    }
}
