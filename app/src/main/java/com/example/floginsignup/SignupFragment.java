package com.example.floginsignup;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SignupFragment extends Fragment {
    private EditText etEmail, etPassword;
    private Button btnSignup;
    private TextView tvLoginLink;
    private FirebaseServices fbs;

    public SignupFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_signup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fbs = FirebaseServices.getInstance();
        etEmail = view.findViewById(R.id.etUsernameSignup);
        etPassword = view.findViewById(R.id.etPasswordSignup);
        btnSignup = view.findViewById(R.id.btnSignup);
        tvLoginLink = view.findViewById(R.id.tvLoginLinkSignup);

        tvLoginLink.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        btnSignup.setOnClickListener(v -> {
            // get what user typed
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString();
            // make sure both fields aren't empty
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(getActivity(), "Please fill in both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // call firebase to make the new account
            fbs.getAuth().createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(authResult -> gotoHome())
                    .addOnFailureListener(e ->
                            Toast.makeText(getActivity(), "Signup failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }

    private void gotoHome() {
        if (getActivity() == null) return;
        Intent i = new Intent(getActivity(), HomeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}
