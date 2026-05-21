package com.example.floginsignup.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.floginsignup.R;
import com.example.floginsignup.model.ActivityItem;
import com.example.floginsignup.ui.util.ActivityIcons;
import com.example.floginsignup.ui.util.TimeAgo;

import java.util.ArrayList;
import java.util.List;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.VH> {

    private final List<ActivityItem> items = new ArrayList<>();

    public void submit(List<ActivityItem> next) {
        items.clear();
        items.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ActivityItem item = items.get(position);
        h.bg.setBackgroundResource(ActivityIcons.bgFor(item.type));
        h.icon.setImageResource(ActivityIcons.iconFor(item.type));
        h.icon.setImageTintList(ContextCompat.getColorStateList(h.itemView.getContext(),
                ActivityIcons.tintFor(item.type)));
        h.title.setText(item.title);
        String when = TimeAgo.format(h.itemView.getContext(), item.timestampMs);
        h.subtitle.setText(when + " · " + item.detail);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView bg, icon;
        final TextView title, subtitle;
        VH(@NonNull View v) {
            super(v);
            bg = v.findViewById(R.id.imgActivityBg);
            icon = v.findViewById(R.id.imgActivityIcon);
            title = v.findViewById(R.id.tvActivityTitle);
            subtitle = v.findViewById(R.id.tvActivitySubtitle);
        }
    }
}
