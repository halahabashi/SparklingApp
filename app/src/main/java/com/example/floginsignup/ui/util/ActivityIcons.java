package com.example.floginsignup.ui.util;

import com.example.floginsignup.R;
import com.example.floginsignup.model.ActivityItem;

public final class ActivityIcons {
    private ActivityIcons() {}

    public static int iconFor(ActivityItem.Type type) {
        switch (type) {
            case CAR_ENTERED: return R.drawable.ic_car;
            case CAR_EXITED:  return R.drawable.ic_logout;
            case SUBSCRIPTION: return R.drawable.ic_card;
            case GATE_OPENED: return R.drawable.ic_shield;
        }
        return R.drawable.ic_car;
    }

    public static int bgFor(ActivityItem.Type type) {
        switch (type) {
            case CAR_ENTERED: return R.drawable.bg_circle_red_soft;
            case CAR_EXITED:  return R.drawable.bg_circle_green_soft;
            case SUBSCRIPTION: return R.drawable.bg_circle_amber_soft;
            case GATE_OPENED: return R.drawable.bg_circle_blue_soft;
        }
        return R.drawable.bg_circle_red_soft;
    }

    public static int tintFor(ActivityItem.Type type) {
        switch (type) {
            case CAR_ENTERED: return R.color.brand_red;
            case CAR_EXITED:  return R.color.brand_green;
            case SUBSCRIPTION: return R.color.brand_amber;
            case GATE_OPENED: return R.color.brand_blue;
        }
        return R.color.brand_red;
    }
}
