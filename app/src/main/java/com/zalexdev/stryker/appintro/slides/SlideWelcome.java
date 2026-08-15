package com.zalexdev.stryker.appintro.slides;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.zalexdev.stryker.R;

public class SlideWelcome extends Fragment {

    private static final long ENTER_MS = 620L;
    private static final float RISE_DP = 26f;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.new_slide_welcome, container, false);

        View logo = view.findViewById(R.id.welcome_logo);
        View title = view.findViewById(R.id.welcome_title);
        View subtitle = view.findViewById(R.id.welcome_subtitle);
        MaterialButton cta = view.findViewById(R.id.welcome_cta);

        float rise = RISE_DP * getResources().getDisplayMetrics().density;
        enter(logo, 120L, rise * 0.5f, 0.72f);
        enter(title, 280L, rise, 1f);
        enter(subtitle, 380L, rise, 1f);
        enter(cta, 500L, rise, 1f);

        cta.setOnClickListener(v -> {
            ViewPager2 pager = requireActivity().findViewById(R.id.view_pager);
            if (pager != null) pager.setCurrentItem(pager.getCurrentItem() + 1);
        });

        return view;
    }

    private void enter(View view, long delay, float fromY, float fromScale) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(fromY);
        if (fromScale != 1f) {
            view.setScaleX(fromScale);
            view.setScaleY(fromScale);
        }
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(delay)
                .setDuration(ENTER_MS)
                .setInterpolator(fromScale != 1f
                        ? new OvershootInterpolator(1.6f)
                        : new DecelerateInterpolator(2f))
                .start();
    }
}
