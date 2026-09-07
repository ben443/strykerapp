package com.zalexdev.stryker.appintro;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;
import com.zalexdev.stryker.R;
import com.zalexdev.stryker.appintro.slides.Slide1;
import com.zalexdev.stryker.appintro.slides.Slide2;
import com.zalexdev.stryker.appintro.slides.Slide3;
import com.zalexdev.stryker.appintro.slides.Slide6Final;
import com.zalexdev.stryker.appintro.slides.SlideEngineSelect;
import com.zalexdev.stryker.appintro.slides.SlidePCheck;
import com.zalexdev.stryker.appintro.slides.SlideQemuInstall;
import com.zalexdev.stryker.appintro.slides.SlideWelcome;
import com.zalexdev.stryker.engine.EngineType;
import com.zalexdev.stryker.utils.EffectBackdrop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppIntroActivity extends FragmentActivity {

    public static final String EXTRA_MIGRATE = "migrate_legacy_chroot";

    public enum Page { WELCOME, CONSENT, ENGINE, PERMS, PCHECK, INSTALL_CHROOT, INSTALL_QEMU, FINAL }

    public boolean isMigration() {
        return getIntent() != null && getIntent().getBooleanExtra(EXTRA_MIGRATE, false);
    }

    private final List<Page> pages = new ArrayList<>(Arrays.asList(
            Page.WELCOME, Page.CONSENT, Page.ENGINE, Page.PERMS, Page.PCHECK,
            Page.INSTALL_CHROOT, Page.FINAL));

    private ViewPager2 mPager;
    private ScreenPagerAdapter pagerAdapter;
    private LinearProgressIndicator progress;
    private MaterialTextView stepLabel;
    private View header;
    private Backdrop lightfall;
    private Backdrop floatingLines;
    private boolean resumed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_intro);

        mPager = findViewById(R.id.view_pager);
        progress = findViewById(R.id.intro_progress);
        stepLabel = findViewById(R.id.intro_step);
        header = findViewById(R.id.intro_header);
        lightfall = new Backdrop(findViewById(R.id.intro_lightfall));
        floatingLines = new Backdrop(findViewById(R.id.intro_floating_lines));

        if (isMigration()) pages.remove(Page.WELCOME);

        mPager.setUserInputEnabled(false);
        mPager.setPageTransformer(new SlideFadeTransformer());
        pagerAdapter = new ScreenPagerAdapter(this, pages);
        mPager.setAdapter(pagerAdapter);
        mPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) { bindProgress(position); }
        });

        ImageView logo = findViewById(R.id.logo);
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        logo.setImageResource(dark ? R.drawable.ic_white : R.drawable.ic_blue);
        logo.setAlpha(0f);
        logo.setScaleX(0.85f);
        logo.setScaleY(0.85f);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(getResources().getInteger(R.integer.motion_long))
                .start();

        bindProgress(0);
    }

    public void applyEngineFlow(EngineType type) {
        int keep = pages.indexOf(Page.ENGINE) + 1;
        while (pages.size() > keep) pages.remove(pages.size() - 1);
        pages.add(Page.PERMS);
        if (type == EngineType.ROOTLESS || type == EngineType.UML) {
            pages.add(Page.INSTALL_QEMU);
        } else {
            pages.add(Page.PCHECK);
            pages.add(Page.INSTALL_CHROOT);
        }
        pages.add(Page.FINAL);
        pagerAdapter.notifyDataSetChanged();
        bindProgress(mPager.getCurrentItem());
    }

    public void jumpToLast() {
        mPager.setCurrentItem(pages.size() - 1);
    }

    private void bindProgress(int position) {
        Page page = position >= 0 && position < pages.size() ? pages.get(position) : null;
        applyChrome(page);
        if (page == Page.WELCOME) return;

        int total = 0;
        int step = 0;
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i) == Page.WELCOME) continue;
            total++;
            if (i <= position) step++;
        }
        if (total <= 0) return;
        step = Math.min(Math.max(step, 1), total);

        if (progress != null) {
            progress.setMax(total);
            progress.setProgressCompat(step, true);
        }
        if (stepLabel != null) {
            stepLabel.setText(getString(R.string.intro_step_of, step, total));
        }
    }

    private void applyChrome(Page page) {
        boolean welcome = page == Page.WELCOME;
        if (header != null) header.setVisibility(welcome ? View.GONE : View.VISIBLE);

        float welcomeStrength = getResources().getInteger(R.integer.welcome_effect_opacity_pct) / 100f;
        float flowStrength = getResources().getInteger(R.integer.intro_effect_opacity_pct) / 100f;
        lightfall.setStrength(welcome ? welcomeStrength : 0f);
        floatingLines.setStrength(welcome ? 0f : flowStrength);

        boolean installing = page == Page.INSTALL_CHROOT || page == Page.INSTALL_QEMU;
        floatingLines.setTargetFps(installing ? 30 : 60);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        lightfall.syncPaused();
        floatingLines.syncPaused();
    }

    @Override
    protected void onPause() {
        resumed = false;
        lightfall.syncPaused();
        floatingLines.syncPaused();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        lightfall.cancel();
        floatingLines.cancel();
        super.onDestroy();
    }

    private final class Backdrop {
        private final View view;
        private final EffectBackdrop effect;
        private ValueAnimator fade;
        private float strength = -1f;
        private boolean wanted;

        Backdrop(@Nullable View view) {
            this.view = view;
            this.effect = view instanceof EffectBackdrop ? (EffectBackdrop) view : null;
        }

        void setStrength(float target) {
            if (effect == null) return;
            if (fade != null) {
                fade.cancel();
                fade = null;
            }
            wanted = target > 0f;
            if (wanted) view.setVisibility(View.VISIBLE);
            syncPaused();

            if (strength < 0f) {
                strength = target;
                effect.setEffectOpacity(target);
                applyVisibility();
                return;
            }
            if (Math.abs(target - strength) < 0.001f) return;

            ValueAnimator a = ValueAnimator.ofFloat(strength, target);
            a.setDuration(getResources().getInteger(R.integer.motion_long));
            a.addUpdateListener(v -> {
                strength = (Float) v.getAnimatedValue();
                effect.setEffectOpacity(strength);
            });
            a.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (fade != animation) return;
                    fade = null;
                    applyVisibility();
                    syncPaused();
                }
            });
            fade = a;
            a.start();
        }

        void setTargetFps(int fps) {
            if (effect != null) effect.setTargetFps(fps);
        }

        void syncPaused() {
            if (effect == null) return;
            effect.setPaused(!(resumed && (wanted || strength > 0.001f)));
        }

        private void applyVisibility() {
            if (view != null && !wanted) view.setVisibility(View.GONE);
        }

        void cancel() {
            if (fade != null) {
                fade.cancel();
                fade = null;
            }
        }
    }

    @Override
    public void onBackPressed() {
    }

    private static class ScreenPagerAdapter extends FragmentStateAdapter {
        private final List<Page> pages;

        ScreenPagerAdapter(@NonNull FragmentActivity a, List<Page> pages) {
            super(a);
            this.pages = pages;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (pages.get(position)) {
                case WELCOME: return new SlideWelcome();
                case CONSENT: return new Slide1();
                case ENGINE: return new SlideEngineSelect();
                case PERMS: return new Slide2();
                case PCHECK: return new SlidePCheck();
                case INSTALL_CHROOT: return new Slide3();
                case INSTALL_QEMU: return new SlideQemuInstall();
                case FINAL:
                default: return new Slide6Final();
            }
        }

        @Override
        public int getItemCount() { return pages.size(); }

        @Override
        public long getItemId(int position) { return pages.get(position).ordinal(); }

        @Override
        public boolean containsItem(long itemId) {
            for (Page p : pages) if (p.ordinal() == itemId) return true;
            return false;
        }
    }

    private static class SlideFadeTransformer implements ViewPager2.PageTransformer {
        @Override
        public void transformPage(@NonNull View page, float position) {
            float abs = Math.abs(position);
            if (abs >= 1f) {
                page.setAlpha(0f);
                page.setTranslationX(0f);
                return;
            }
            page.setAlpha(1f - abs);
            page.setTranslationX(-position * page.getWidth() * 0.12f);
        }
    }
}
