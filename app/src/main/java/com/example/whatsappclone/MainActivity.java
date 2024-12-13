package com.example.whatsappclone;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Inflate the layout using binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

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
