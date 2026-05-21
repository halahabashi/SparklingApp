package com.example.floginsignup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginFragment extends Fragment {
    private EditText etEmail, etPassword;
    private TextView tvSignupLink;
    private TextView tvForgotPasswordLink;
    private Button btnLoginAsAdmin, btnLoginAsClient;
    private FirebaseServices fbs;

    public static final String EXTRA_MOCK_ROLE = "mock_role";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_CLIENT = "client";

    public static final String PREFS = "session_prefs";
    public static final String KEY_ROLE = "role";

    public LoginFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fbs = FirebaseServices.getInstance();
        etEmail = view.findViewById(R.id.etUsernameLogin);
        etPassword = view.findViewById(R.id.etPasswordLogin);
        btnLoginAsAdmin = view.findViewById(R.id.btnLoginAsAdmin);
        btnLoginAsClient = view.findViewById(R.id.btnLoginAsClient);
        tvForgotPasswordLink = view.findViewById(R.id.tvForgotPasswordLinkLogin);
        tvSignupLink = view.findViewById(R.id.tvSignupLinkLogin);

        tvSignupLink.setOnClickListener(v -> gotoSignupFragment());
        tvForgotPasswordLink.setOnClickListener(v -> gotoForgotPasswordFragment());

        btnLoginAsAdmin.setOnClickListener(v -> attemptLogin(ROLE_ADMIN));
        btnLoginAsClient.setOnClickListener(v -> attemptLogin(ROLE_CLIENT));
    }

    private void attemptLogin(String role) {
        // grab what the user typed
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString();
        // make sure they actually filled something in
        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(getActivity(), "Please fill in both fields", Toast.LENGTH_SHORT).show();
            return;
        }
        // disable buttons so user doesn't spam click
        setButtonsEnabled(false);
        fbs.getAuth().signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> {
                    persistRole(role);
                    gotoHomeAs(role);
                })
                .addOnFailureListener(e -> {
                    setButtonsEnabled(true);
                    Toast.makeText(getActivity(), "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setButtonsEnabled(boolean enabled) {
        btnLoginAsAdmin.setEnabled(enabled);
        btnLoginAsClient.setEnabled(enabled);
    }

    private void persistRole(String role) {
        if (getActivity() == null) return;
        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ROLE, role).apply();
    }

    private void gotoSignupFragment() {
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.frameLayoutMain, new SignupFragment());
        ft.addToBackStack(null);
        ft.commit();
    }

    private void gotoForgotPasswordFragment() {
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.frameLayoutMain, new ForgotPasswordFragment());
        ft.addToBackStack(null);
        ft.commit();
    }

    private void gotoHomeAs(String role) {
        if (getActivity() == null) return;
        // open the home screen and clear back stack so back button doesn't go to login
        Intent i = new Intent(getActivity(), HomeActivity.class);
        i.putExtra(EXTRA_MOCK_ROLE, role);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}
