package com.lizechao.thanos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import pers.lizechao.android_lib.ui.common.BaseActivity;
import pers.lizechao.android_lib.ui.manager.StatusBarManager;

public class CrashActivity extends BaseActivity<com.lizechao.thanos.databinding.ActivityCrashBinding> {
    //SurfaceView 管理
    private SurfaceManager surfaceManager;
    //粉碎特效实现
    private CrashAnim crashAnim;
    private List<View> dismissView = new ArrayList<>();
    Random random = new Random();

    //打响指动画
    private AnimationDrawable doAnim;
    private AnimationDrawable backAnim;

    private boolean isPlay = false;
    private boolean isVanish = false;

    @Override
    protected void initExtraView() {
        super.initExtraView();
        viewBind.surfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        viewBind.surfaceView.setZOrderMediaOverlay(true);
        viewBind.surfaceView.setZOrderOnTop(true);
        surfaceManager = new SurfaceManager(viewBind.surfaceView);
        surfaceManager.waitFor();
        crashAnim = new CrashAnim(surfaceManager);
        crashAnim.setOnAnimListener(new CrashAnim.onAnimListener() {
            @Override
            public void onEnd() {
                isPlay = false;
                isVanish = true;
                viewBind.doIt.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStart() {
                for (View view : dismissView) {
                    view.setVisibility(View.INVISIBLE);
                }
            }
        });

        viewBind.doIt.setOnClickListener(v -> {
            if (isPlay) {
                return;
            }
            isPlay = true;
            if (!isVanish) {
                playThumnailAnim();

            } else {
                playResumeThumnailAnim();
            }

        });
        doAnim = loadAnim(R.raw.thanos_snap);
        backAnim = loadAnim(R.raw.thanos_time);
    }

    private void playResumeThumnailAnim() {
        viewBind.anim.setBackground(backAnim);
        viewBind.doIt.setVisibility(View.INVISIBLE);
        viewBind.anim.postDelayed(new Runnable() {
            @Override
            public void run() {
                playReliveAnim();
            }
        }, 2500);
        backAnim.start();
    }

    private void playThumnailAnim() {
        viewBind.anim.setBackground(doAnim);
        viewBind.doIt.setVisibility(View.INVISIBLE);
        viewBind.anim.postDelayed(new Runnable() {
            @Override
            public void run() {
                playVanishAnim();
            }
        }, 2500);
        doAnim.start();
    }

    private void playReliveAnim() {
        for (View view : dismissView) {
            view.setVisibility(View.VISIBLE);
            Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.show_anim);
            view.setAnimation(animation);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    isPlay = false;
                    isVanish = false;
                    viewBind.doIt.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            animation.start();
        }
    }

    private void playVanishAnim() {
        if (crashAnim.isPlayAnim())
            return;
        dismissView.clear();
        surfaceManager.awake();
        for (int i = 0; i < viewBind.content.getChildCount(); i++) {
            dismissView.add(viewBind.content.getChildAt(i));
        }
        for (int i = 0; i < viewBind.content.getChildCount() / 2; i++) {
            dismissView.remove(random.nextInt(dismissView.size()));
        }


        for (View view : dismissView) {
            crashAnim.addAnimViewRecursion(view, view.getX(), view.getY());
        }
        crashAnim.startAnim();
    }

    private AnimationDrawable loadAnim(int rawId) {
        BitmapRegionDecoder decoder = null;
        try {
            decoder = BitmapRegionDecoder.newInstance(getResources().openRawResource(rawId), true);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        AnimationDrawable animationDrawable = new AnimationDrawable();
        Rect rect = new Rect();
        int unitWith = decoder.getWidth() / 48;
        int unitHeight = decoder.getHeight();
        for (int i = 0; i < 48; i++) {
            rect.set(i * unitWith, 0, (i + 1) * unitWith, unitHeight);
            Bitmap bitmap = decoder.decodeRegion(rect, new BitmapFactory.Options());
            animationDrawable.addFrame(new BitmapDrawable(getResources(), bitmap), 1000 / 24);
        }
        animationDrawable.setOneShot(true);
        return animationDrawable;
    }


    @Override
    protected StatusBarManager.BarState getBarState() {
        return StatusBarManager.BarState.Full;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_crash;
    }
}
