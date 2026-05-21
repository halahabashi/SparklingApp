package com.example.floginsignup.ui.util;

import android.content.Context;

import com.example.floginsignup.R;

public final class TimeAgo {
    private TimeAgo() {}

    // turn a timestamp into something like "5 minutes ago"
    public static String format(Context ctx, long timestampMs) {
        long diff = System.currentTimeMillis() - timestampMs;
        // less than a minute
        if (diff < 60_000) return ctx.getString(R.string.time_just_now);
        long minutes = diff / 60_000;
        if (minutes < 60) {
            if (minutes == 1) {
                return ctx.getString(R.string.time_minute_ago, 1);
            } else {
                return ctx.getString(R.string.time_minutes_ago, (int) minutes);
            }
        }
        // an hour or more
        long hours = minutes / 60;
        if (hours == 1) {
            return ctx.getString(R.string.time_hour_ago, 1);
        } else {
            return ctx.getString(R.string.time_hours_ago, (int) hours);
        }
    }
}
