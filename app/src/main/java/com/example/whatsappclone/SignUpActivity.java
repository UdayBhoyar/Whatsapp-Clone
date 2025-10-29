package com.example.whatsappclone;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.whatsappclone.Models.Users;
import com.example.whatsappclone.databinding.ActivitySignUpBinding;
import com.example.whatsappclone.utils.RSAKeyManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import android.content.Intent;

import java.security.KeyPair;

public class SignUpActivity extends AppCompatActivity {
    ActivitySignUpBinding binding;
    private FirebaseAuth mAuth;
    FirebaseDatabase database;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater()); // Gets all the views id no need to use find view by id for all
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        progressDialog = new ProgressDialog(SignUpActivity.this);
        progressDialog.setTitle("Creating Account");
        progressDialog.setMessage("We're creating your account.");

        binding.signupbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get input values
                String email = binding.txtemail.getText().toString().trim();
                String username = binding.username.getText().toString().trim();
                String password = binding.txtpassword.getText().toString().trim();

                // Validate inputs
                if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(SignUpActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return; // Exit if inputs are not valid
                }

                // Proceed with sign-up logic
                progressDialog.show();
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressDialog.dismiss(); // Dismiss the dialog

                        if(task.isSuccessful()){
                            try {
                                // Generate RSA key pair for the new user
                                KeyPair keyPair = RSAKeyManager.generateKeyPair();
                                
                                // Save private key locally on device
                                RSAKeyManager.saveKeyPair(SignUpActivity.this, keyPair);
                                
                                // Get public key as string for Firebase
                                String publicKeyStr = RSAKeyManager.publicKeyToString(keyPair.getPublic());
                                
                                // Create user object
                                Users users = new Users(username, email, password);
                                String id = task.getResult().getUser().getUid();
                                
                                // Save user data to Firebase
                                database.getReference().child("Users").child(id).setValue(users);
                                
                                // Save public key to Firebase
                                database.getReference().child("PublicKeys").child(id).setValue(publicKeyStr);
                                
                                Toast.makeText(SignUpActivity.this, "Sign Up Successful", Toast.LENGTH_SHORT).show();
                                
                                // Redirect to MainActivity after successful signup
                                Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                                
                            } catch (Exception e) {
                                Log.e("SignUpActivity", "Failed to generate encryption keys", e);
                                Toast.makeText(SignUpActivity.this, "Failed to setup encryption: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(SignUpActivity.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        binding.txtalreadyhaveaccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(SignUpActivity.this,SignInActivity.class);
                startActivity(intent);
            }
        });
    }
}
