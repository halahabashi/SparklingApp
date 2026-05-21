package com.example.floginsignup.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.content.Context;

import com.example.floginsignup.FirebaseServices;
import com.example.floginsignup.LoginFragment;
import com.example.floginsignup.MainActivity;
import com.example.floginsignup.R;
import com.example.floginsignup.Session;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView name = view.findViewById(R.id.tvProfileName);
        TextView email = view.findViewById(R.id.tvProfileEmail);
        MaterialButton signOut = view.findViewById(R.id.btnSignOut);

        Session s = Session.get();
        FirebaseUser user = FirebaseServices.getInstance().getAuth().getCurrentUser();
        // try to get the user's email, otherwise show a fake one based on role
        String mail;
        if (user != null && user.getEmail() != null) {
            mail = user.getEmail();
        } else if (s.isAdmin()) {
            mail = "admin@smartparking.local";
        } else {
            mail = "client@smartparking.local";
        }
        name.setText(s.getUserName());
        email.setText(mail);

        // sign out button: logout from firebase + clear saved role + go back to start
        signOut.setOnClickListener(v -> {
            FirebaseServices.getInstance().getAuth().signOut();
            if (getActivity() != null) {
                getActivity().getSharedPreferences(LoginFragment.PREFS, Context.MODE_PRIVATE)
                        .edit().remove(LoginFragment.KEY_ROLE).apply();
                Intent i = new Intent(getActivity(), MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            }
        });
    }
}
