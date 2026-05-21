package com.example.floginsignup;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // if user already logged in, skip the welcome/login screens
        if (FirebaseServices.getInstance().getAuth().getCurrentUser() != null) {
            // read the saved role (admin by default)
            String savedRole = getSharedPreferences(LoginFragment.PREFS, MODE_PRIVATE)
                    .getString(LoginFragment.KEY_ROLE, LoginFragment.ROLE_ADMIN);
            android.content.Intent i = HomeActivity.intent(this);
            i.putExtra(LoginFragment.EXTRA_MOCK_ROLE, savedRole);
            startActivity(i);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        // show the welcome screen first
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.frameLayoutMain, new WelcomeFragment());
        ft.commit();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}