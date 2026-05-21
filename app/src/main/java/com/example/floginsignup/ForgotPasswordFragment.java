package com.example.floginsignup;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ForgotPasswordFragment extends Fragment {

    private FirebaseServices fbs;
    private EditText etEmail;
    private Button btnReset;

    public ForgotPasswordFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fbs = FirebaseServices.getInstance();
        etEmail = view.findViewById(R.id.etEmailForgotPassword);
        btnReset = view.findViewById(R.id.btnResetForgotPassword);

        btnReset.setOnClickListener(v -> {
            // read email from box
            String email = etEmail.getText().toString().trim();
            // check it's not empty
            if (email.isEmpty()) {
                Toast.makeText(getActivity(), "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            // ask firebase to send the reset email
            fbs.getAuth().sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getActivity(), "Check your email", Toast.LENGTH_SHORT).show();
                        } else {
                            String msg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(getActivity(), "Failed: " + msg, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
