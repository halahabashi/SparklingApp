package com.example.floginsignup;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.floginsignup.ui.ActivityFragment;
import com.example.floginsignup.ui.DashboardFragment;
import com.example.floginsignup.ui.ParkingFragment;
import com.example.floginsignup.ui.ProfileFragment;

public class HomeActivity extends AppCompatActivity {

    private NavItem[] items;
    private View navPill;
    private LinearLayout navRow;
    private int selectedIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // figure out if we logged in as admin or client
        String role = getIntent().getStringExtra(LoginFragment.EXTRA_MOCK_ROLE);
        if (LoginFragment.ROLE_CLIENT.equals(role)) {
            Session.get().setRole(Session.Role.CLIENT);
        } else if (LoginFragment.ROLE_ADMIN.equals(role)) {
            Session.get().setRole(Session.Role.ADMIN);
        }

        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homeRoot), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        navPill = findViewById(R.id.navPill);
        navRow = findViewById(R.id.navRow);

        items = new NavItem[]{
                new NavItem(findViewById(R.id.navHome),    R.drawable.ic_nav_home,    R.string.nav_home),
                new NavItem(findViewById(R.id.navParking), R.drawable.ic_nav_parking, R.string.nav_parking),
                new NavItem(findViewById(R.id.navActivity),R.drawable.ic_nav_activity,R.string.nav_activity),
                new NavItem(findViewById(R.id.navProfile), R.drawable.ic_nav_profile, R.string.nav_profile)
        };

        for (int i = 0; i < items.length; i++) {
            final int index = i;
            items[i].init(this);
            items[i].container.setOnClickListener(v -> select(index));
        }

        if (savedInstanceState == null) select(0);
    }

    public void selectTab(int index) {
        if (index < 0 || index >= items.length) return;
        select(index);
    }

    private void select(int index) {
        // don't do anything if same tab tapped again
        if (selectedIndex == index) return;
        boolean firstSelection = selectedIndex == -1;
        selectedIndex = index;

        // update colors on all tabs
        for (int i = 0; i < items.length; i++) {
            items[i].setActiveColors(this, i == index, !firstSelection);
        }
        movePillTo(index, !firstSelection);

        // pick which fragment to show
        Fragment f;
        switch (index) {
            case 1: f = new ParkingFragment(); break;
            case 2: f = new ActivityFragment(); break;
            case 3: f = new ProfileFragment(); break;
            case 0:
            default: f = new DashboardFragment(); break;
        }
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        if (!firstSelection) {
            tx.setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out);
        }
        tx.replace(R.id.homeContainer, f).commit();
    }

    private void movePillTo(int index, boolean animate) {
        View target = items[index].container;
        Runnable position = () -> {
            float targetX = navRow.getX() + target.getX();
            float targetY = navRow.getY() + target.getY();
            int targetW = target.getWidth();
            int targetH = target.getHeight();

            ViewGroup.LayoutParams lp = navPill.getLayoutParams();
            if (lp.width != targetW || lp.height != targetH) {
                lp.width = targetW;
                lp.height = targetH;
                navPill.setLayoutParams(lp);
            }
            navPill.setY(targetY);

            if (!animate) {
                navPill.setX(targetX);
                navPill.setVisibility(View.VISIBLE);
            } else {
                navPill.animate()
                        .x(targetX)
                        .setDuration(280)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
        };
        if (target.getWidth() == 0) {
            target.post(position);
        } else {
            position.run();
        }
    }

    public static Intent intent(Context ctx) {
        return new Intent(ctx, HomeActivity.class);
    }

    private static class NavItem {
        final LinearLayout container;
        final int iconRes;
        final int labelRes;
        ImageView icon;
        TextView label;
        boolean isActive = false;
        ValueAnimator colorAnim;

        NavItem(LinearLayout container, int iconRes, int labelRes) {
            this.container = container;
            this.iconRes = iconRes;
            this.labelRes = labelRes;
        }

        void init(Context ctx) {
            float density = ctx.getResources().getDisplayMetrics().density;
            container.removeAllViews();
            container.setOrientation(LinearLayout.VERTICAL);
            container.setGravity(Gravity.CENTER);
            container.setPadding(dp(density, 8), dp(density, 14), dp(density, 8), dp(density, 14));

            icon = new ImageView(ctx);
            icon.setImageResource(iconRes);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(density, 20), dp(density, 20));
            icon.setLayoutParams(iconLp);

            label = new TextView(ctx);
            label.setText(labelRes);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            label.setAllCaps(true);
            label.setLetterSpacing(0.05f);
            LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lblLp.topMargin = dp(density, 4);
            label.setLayoutParams(lblLp);

            container.addView(icon);
            container.addView(label);
        }

        void setActiveColors(Context ctx, boolean active, boolean animate) {
            int activeColor = ContextCompat.getColor(ctx, R.color.white);
            int inactiveColor = ContextCompat.getColor(ctx, R.color.text_muted);
            int target = active ? activeColor : inactiveColor;
            int from = isActive ? activeColor : inactiveColor;

            label.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

            if (colorAnim != null) colorAnim.cancel();
            if (!animate || from == target) {
                icon.setImageTintList(ColorStateList.valueOf(target));
                label.setTextColor(target);
            } else {
                colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), from, target);
                colorAnim.setDuration(280);
                colorAnim.setInterpolator(new AccelerateDecelerateInterpolator());
                colorAnim.addUpdateListener(a -> {
                    int c = (int) a.getAnimatedValue();
                    icon.setImageTintList(ColorStateList.valueOf(c));
                    label.setTextColor(c);
                });
                colorAnim.start();
            }
            isActive = active;
        }

        private static int dp(float density, int v) { return Math.round(v * density); }
    }
}
