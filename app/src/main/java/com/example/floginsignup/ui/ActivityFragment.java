package com.example.floginsignup.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.floginsignup.R;
import com.example.floginsignup.Session;
import com.example.floginsignup.api.ApiCallback;
import com.example.floginsignup.api.ApiClient;
import com.example.floginsignup.model.ActivityItem;

import java.util.ArrayList;
import java.util.List;

public class ActivityFragment extends Fragment {

    private ActivityAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView rv = view.findViewById(R.id.rvActivity);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ActivityAdapter();
        rv.setAdapter(adapter);

        ApiClient.get().getActivityLog(new ApiCallback<List<ActivityItem>>() {
            @Override public void onSuccess(List<ActivityItem> data) {
                if (!isAdded()) return;
                adapter.submit(filterForRole(data));
            }
            @Override public void onError(Throwable e) { /* show empty */ }
        });
    }

    // admin sees everything, clients only see their own stuff
    private List<ActivityItem> filterForRole(List<ActivityItem> all) {
        Session s = Session.get();
        if (s.isAdmin()) return all;
        List<ActivityItem> mine = new ArrayList<>();
        String me = s.getUserName();
        // loop through and keep only my items
        for (ActivityItem a : all) {
            if (me.equals(a.detail)) mine.add(a);
        }
        return mine;
    }
}
